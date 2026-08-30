package com.bridgetalk.app.sender

import com.bridgetalk.app.SendDraftRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Real production transport: HTTPS POST to the BridgeTalk backend. The
 * Android app only ever holds a Firebase ID token; the Firebase Admin SDK
 * on the backend infers the operator's uid. The transport attaches the
 * optional bearer token and an [Idempotency-Key] header.
 */
internal class OkHttpSendTransport(
    private val baseUrl: String,
    private val idTokenProvider: suspend () -> String? = { null },
    private val client: OkHttpClient = defaultClient(),
    private val timeoutMillis: Long = 12_000L,
) : HttpSendTransport {

    override suspend fun send(
        callId: String,
        request: SendDraftRequest,
    ): HttpSendTransport.Result = withContext(Dispatchers.IO) {
        withTimeout(timeoutMillis) {
            val body = JSONObject().apply {
                put("message", request.message)
                put("idempotencyKey", request.idempotencyKey)
                put("languageCode", "vi-VN")
                put("senderConsent", false)
                put("recipientConsent", false)
            }.toString().toRequestBody(JSON_MEDIA_TYPE)

            val url = baseUrl.trimEnd('/') + "/api/calls/" + callId + "/send"
            val builder = Request.Builder()
                .url(url)
                .post(body)
                .header("Idempotency-Key", request.idempotencyKey)
                .header("Accept", "application/json")

            val token = idTokenProvider()
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer " + token)
            }

            try {
                client.newCall(builder.build()).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        parseAccepted(raw)
                    } else if (response.code in 400..499) {
                        HttpSendTransport.Result.Rejected(parseError(raw, response.code))
                    } else {
                        // 5xx and unexpected codes: surface as a transport failure
                        // so the orchestrator can fall back to on-device TTS.
                        throw IOException("backend returned HTTP " + response.code)
                    }
                }
            } catch (io: IOException) {
                throw io
            } catch (other: Throwable) {
                throw IOException(other.message ?: "transport failure", other)
            }
        }
    }

    private fun parseAccepted(raw: String): HttpSendTransport.Result.Accepted {
        return try {
            val obj = JSONObject(raw)
            val voiceOutput = obj.optJSONObject("voiceOutput")
            val source = voiceOutput?.optString("source").orEmpty()
            val message = if (source == "cloud") {
                "Đã gửi · máy chủ đã tổng hợp giọng nói cho người nhận."
            } else {
                "Đã gửi · âm thanh đã được gửi đến máy chủ."
            }
            HttpSendTransport.Result.Accepted(userMessage = message)
        } catch (e: JSONException) {
            HttpSendTransport.Result.Accepted(userMessage = "Đã gửi.")
        }
    }

    private fun parseError(raw: String, code: Int): String {
        return try {
            val obj = JSONObject(raw)
            val reason = obj.optString("error").ifBlank { "HTTP_$code" }
            "Máy chủ từ chối: $reason"
        } catch (e: JSONException) {
            "Máy chủ từ chối (HTTP $code)."
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }
}
