package com.bridgetalk.app

import kotlinx.coroutines.delay

/** Debug-only preview transport. It is never compiled into the release variant. */
internal fun defaultDraftMessageSender(): DraftMessageSender = DebugPreviewDraftMessageSender

private object DebugPreviewDraftMessageSender : DraftMessageSender {
    override suspend fun send(request: SendDraftRequest): SendDraftResult {
        delay(350)
        return SendDraftResult.Accepted(
            userMessage = "Đã gửi trong bản demo cục bộ · production vẫn cần backend WebRTC/TTS",
        )
    }
}
