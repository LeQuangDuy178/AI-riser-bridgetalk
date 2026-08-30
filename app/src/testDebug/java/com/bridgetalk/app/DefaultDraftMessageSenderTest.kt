package com.bridgetalk.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDraftMessageSenderTest {
    @Test
    fun debugSenderAcceptsTheLocalPreviewFlow() = runBlocking {
        val result = defaultDraftMessageSender().send(
            SendDraftRequest(message = "SOS", idempotencyKey = "debug-key"),
        )

        assertTrue(result is SendDraftResult.Accepted)
        assertTrue((result as SendDraftResult.Accepted).userMessage.contains("demo cục bộ"))
    }
}
