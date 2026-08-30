package com.bridgetalk.app.sender

import com.bridgetalk.app.SendDraftRequest
import java.io.IOException

/**
 * Transport the Android client uses to deliver a confirmed draft to the
 * BridgeTalk backend. The interface is seam so the orchestrator
 * ([ConfirmDraftMessageSender]) is unit-testable with a fake transport in
 * the JVM test source set.
 */
internal interface HttpSendTransport {
    sealed interface Result {
        data class Accepted(val userMessage: String) : Result
        data class Rejected(val userMessage: String) : Result
    }

    @Throws(IOException::class)
    suspend fun send(callId: String, request: SendDraftRequest): Result
}
