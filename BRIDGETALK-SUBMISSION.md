# BridgeTalk — AI Riser x LotusHacks submission note

## One-line pitch

BridgeTalk helps a person who cannot speak or hear complete a real-time 1-to-1 conversation: see what the other person says, compose a reply with Morse, review an AI suggestion, and send only when ready.

## Two-minute demo story

1. Open the BridgeTalk Android app and sign in.
2. Create a Bridge ID and add the demo friend An Nam.
3. Start and accept a 1-to-1 call.
4. Show the source caption and English translation together.
5. Press and release the large Morse control to make a draft.
6. Select one suggestion, edit the draft if needed, and call out that nothing is sent yet.
7. Press `Gửi nội dung đã xác nhận` to demonstrate the consent boundary.
8. End the call and show that transcript saving requires participant consent.

## Product guardrails shown in the demo

- The app is not GSM, FaceTime, an emergency service, or a sign-language interpreter.
- The original caption remains visible beside the translation.
- Gemini suggestions do not replace the draft and do not speak automatically.
- Text-to-Speech starts only after B presses Send.
- Demo state is in memory; it is not presented as Firebase persistence.

## Scope status

The current Android slice is a native Kotlin/Jetpack Compose UI prototype. Firebase Auth, Firestore, FCM, WebRTC media, Google Cloud Speech/Translation/TTS and the server-side Gemini adapter are integration milestones after the UI walkthrough. The submission should describe this honestly as a prototype unless those adapters are connected and tested with real credentials in a secured backend.
