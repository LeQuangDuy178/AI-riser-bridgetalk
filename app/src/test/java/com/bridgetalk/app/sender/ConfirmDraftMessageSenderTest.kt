package com.bridgetalk.app.sender

import com.bridgetalk.app.SendDraftRequest
import com.bridgetalk.app.SendDraftResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class ConfirmDraftMessageSenderTest {

    private val request = SendDraftRequest(
        message = "Xin chào",
        idempotencyKey = "test-key-1",
    )

    @Test
    fun acceptedResultIsReturnedWhenTheBackendAccepts() = runTest {
        val speaker = RecordingSpeaker()
        val transport = FakeTransport(HttpSendTransport.Result.Accepted("ok"))
        val sender = ConfirmDraftMessageSender(transport, "call-1", VoiceSpeaker { speaker.speak(it) })

        val result = sender.send(request)

        assertTrue(result is SendDraftResult.Accepted)
        assertEquals(1, transport.callCount.get())
        assertEquals(0, speaker.count)
    }

    @Test
    fun rejectedResultIsReturnedWhenTheBackendRejects() = runTest {
        val speaker = RecordingSpeaker()
        val transport = FakeTransport(HttpSendTransport.Result.Rejected("INVALID_MESSAGE"))
        val sender = ConfirmDraftMessageSender(transport, "call-1", VoiceSpeaker { speaker.speak(it) })

        val result = sender.send(request)

        assertTrue(result is SendDraftResult.Rejected)
        val rejected = result as SendDraftResult.Rejected
        assertTrue(rejected.userMessage.contains("INVALID_MESSAGE"))
        assertEquals(0, speaker.count)
    }

    @Test
    fun offlineFallbackSpeaksAndAcceptsWhenTheTransportThrowsIoException() = runTest {
        val speaker = RecordingSpeaker()
        val transport = ThrowingTransport(IOException("connection refused"))
        val sender = ConfirmDraftMessageSender(transport, "call-1", VoiceSpeaker { speaker.speak(it) })

        val result = sender.send(request)

        assertTrue("Expected Accepted, got $result", result is SendDraftResult.Accepted)
        assertEquals(1, speaker.count)
        assertEquals("Xin chào", speaker.lastSpoken)
    }

    @Test
    fun offlineFallbackNeverSpeaksIfTheTtsVoiceItselfThrows() = runTest {
        val transport = ThrowingTransport(IOException("offline"))
        val sender = ConfirmDraftMessageSender(
            transport = transport,
            callId = "call-1",
            offlineSpeaker = VoiceSpeaker { throw IllegalStateException("tts broken") },
        )

        // A TTS crash must not turn a successful offline send into a rejection.
        val result = sender.send(request)
        assertTrue(result is SendDraftResult.Accepted)
    }

    @Test
    fun cancellationIsReportedAsRejectedAndDoesNotSpeakOffline() = runTest {
        val speaker = RecordingSpeaker()
        val transport = CancellingTransport()
        val sender = ConfirmDraftMessageSender(transport, "call-1", VoiceSpeaker { speaker.speak(it) })

        val result = sender.send(request)

        assertTrue(result is SendDraftResult.Rejected)
        assertEquals(0, speaker.count)
    }

    private class RecordingSpeaker {
        var count = 0
        var lastSpoken: String? = null
        fun speak(text: String) {
            count += 1
            lastSpoken = text
        }
    }

    private class FakeTransport(
        private val result: HttpSendTransport.Result,
    ) : HttpSendTransport {
        val callCount = AtomicInteger(0)
        override suspend fun send(callId: String, request: SendDraftRequest): HttpSendTransport.Result {
            callCount.incrementAndGet()
            return result
        }
    }

    private class ThrowingTransport(private val error: Throwable) : HttpSendTransport {
        override suspend fun send(callId: String, request: SendDraftRequest): HttpSendTransport.Result {
            throw error
        }
    }

    private class CancellingTransport : HttpSendTransport {
        override suspend fun send(callId: String, request: SendDraftRequest): HttpSendTransport.Result {
            throw kotlinx.coroutines.CancellationException("cancelled")
        }
    }
}
