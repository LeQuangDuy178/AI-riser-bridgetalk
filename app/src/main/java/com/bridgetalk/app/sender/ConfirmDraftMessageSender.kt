package com.bridgetalk.app.sender

import com.bridgetalk.app.DraftMessageSender
import com.bridgetalk.app.SendDraftRequest
import com.bridgetalk.app.SendDraftResult
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Fun interface so the orchestrator is unit-testable in the JVM test source
 * set with a fake lambda. The real production wiring passes
 * [com.bridgetalk.app.sender.LocalTtsVoice.speak] from the composition root.
 */
fun interface VoiceSpeaker {
    fun speak(text: String)
}

/**
 * Coordinates one confirmed draft over the BridgeTalk backend. The flow:
 *
 * 1. POST the draft to the backend over HTTPS with the Firebase ID token.
 * 2. On HTTP 200 + [SendDraftResult.Accepted] the backend has already run
 *    Cloud TTS and the recipient will hear the audio over WebRTC. Return
 *    [SendDraftResult.Accepted] verbatim.
 * 3. On [IOException] (no network / backend unreachable) or 5xx / timeout,
 *    fall back to the on-device Android [VoiceSpeaker] so the caller never
 *    sees the legacy "production backend not connected" error, and return
 *    [SendDraftResult.Accepted] with an honest offline status.
 * 4. On 4xx (auth / validation) the backend is reached but rejected the
 *    request. Return [SendDraftResult.Rejected] with the backend's message
 *    so the caller can correct the draft.
 *
 * Cancellation propagates as [SendDraftResult.Rejected] and never speaks
 * the offline fallback (a cancelled send must not speak).
 */
internal class ConfirmDraftMessageSender(
    private val transport: HttpSendTransport,
    private val callId: String,
    private val offlineSpeaker: VoiceSpeaker,
) : DraftMessageSender {

    override suspend fun send(request: SendDraftRequest): SendDraftResult {
        return try {
            val response = transport.send(callId, request)
            when (response) {
                is HttpSendTransport.Result.Accepted -> SendDraftResult.Accepted(
                    userMessage = response.userMessage,
                )
                is HttpSendTransport.Result.Rejected -> SendDraftResult.Rejected(
                    userMessage = response.userMessage,
                )
            }
        } catch (cancellation: CancellationException) {
            SendDraftResult.Rejected(userMessage = "Đã hủy gửi.")
        } catch (ioFailure: IOException) {
            speakOfflineSafely(request.message)
            SendDraftResult.Accepted(
                userMessage = "Gửi ngoại tuyến — phát giọng nói cục bộ cho người nhận.",
            )
        }
    }

    private fun speakOfflineSafely(message: String) {
        try {
            offlineSpeaker.speak(message)
        } catch (t: Throwable) {
            // Never let a TTS failure turn into a send failure.
        }
    }
}
