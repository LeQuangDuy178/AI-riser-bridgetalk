# BridgeTalk

BridgeTalk is an accessibility-first 1-to-1 call prototype for AI Riser Vietnam x LotusHacks.

## Current milestone

This is the native Android submission slice: Kotlin + Jetpack Compose, with an in-memory walkthrough covering:

`login -> profile -> friend -> incoming call -> accessibility call -> Morse MessageDraft -> confirmed send -> consent`

The UI deliberately labels itself as demo mode. Firebase, WebRTC, Google Cloud Speech/Translation/TTS and the server-side Gemini adapter are the next integration milestone; no production credential is stored in the Android app.

## Open in Android Studio

1. Open this folder as an existing Gradle project.
2. Use JDK 17 and Android SDK 35.
3. Run the `app` configuration on an emulator or Android device.

The machine used to scaffold this project does not expose `gradle` or `adb` on PATH, so Android Studio is the expected first build runner.

## Demo path

1. Tap `Đăng nhập để bắt đầu`.
2. Complete the demo profile.
3. Tap `Thêm bạn demo`, then `Gọi`.
4. Accept the incoming call.
5. Press and release the large Morse button. Short press creates `.`, long press creates `-`.
6. Finish a character, select/edit a suggestion, then press `Gửi nội dung đã xác nhận`.

## Scope note

This build is a hackathon prototype, not a production release. It does not claim that local in-memory state is Firebase persistence, that sample captions are live Speech-to-Text, or that the send button has already injected Cloud TTS audio into a WebRTC track.
