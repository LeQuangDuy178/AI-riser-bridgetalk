package com.bridgetalk.app

// The legacy "ProductionSenderRequired" stub that hard-coded
// "Gửi chưa khả dụng: production backend WebRTC/TTS chưa được cấu hình."
// lived here. It was the source of the "cannot be sent because it is not
// connected to backend webrtc/tts" error reported in M-3 of the
// Implementation Readiness Review and has been removed. The release build
// now uses com.bridgetalk.app.sender.ConfirmDraftMessageSender, wired in
// MainActivity.onCreate, which calls the real BridgeTalk backend and falls
// back to the on-device Android TextToSpeech engine when the backend is
// unreachable. See app/src/main/.../sender/ and the ConfirmDraftMessageSenderTest
// unit test for the new behaviour.
