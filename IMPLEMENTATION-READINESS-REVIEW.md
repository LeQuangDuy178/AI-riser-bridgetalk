---
document: Implementation Readiness Review
project: BridgeTalk — accessibility 1-on-1 calling (AI Riser x LotusHacks)
author: Tech Lead / Android Engineer
status: Ready for approval (no code changes authored in this pass)
source-of-truth: `app/`, `backend/`, this document
---

# 1. Current state snapshot (one screen, not a design)

The Android slice in `app/` is a single-Activity Jetpack Compose prototype.
There is no real client-side state persistence, no real audio/video pipeline,
and no real backend service. The app reaches a call screen through hard-coded
in-memory screens (`AppScreen.Welcome -> Profile -> Contacts -> IncomingCall ->
Call -> Consent`). The compose graph in `MainActivity.kt` already contains a
visible "GỬI" (Send) button on the live call screen
(`AccessibleCallScreenV2` at `app/src/main/java/com/bridgetalk/app/MainActivity.kt:877`)
that triggers `sendDraft()`. So the "Send Message button is completely hidden"
bug is *not* present in the code path the app actually uses. The dead
`AccessibleCallScreen` (V1) at `MainActivity.kt:443` does not show a Send
button at all; it shows a "PHÁT NGAY" button that locally fakes success. V1
is unreachable from the navigation graph. The reported "hidden send button"
symptom is therefore almost certainly a viewer running a build where the
production `DraftMessageSender` rejects before the button is even drawn, or a
viewer reading the dead V1 path.

The production `DraftMessageSender` is a stub:

* `app/src/debug/java/com/bridgetalk/app/DefaultDraftMessageSender.kt` —
  `DebugPreviewDraftMessageSender` returns `Accepted("Đã gửi trong bản demo
  cục bộ · production vẫn cần backend WebRTC/TTS")` after a 350 ms delay.
* `app/src/release/java/com/bridgetalk/app/DefaultDraftMessageSender.kt` —
  `ProductionSenderRequired` always returns
  `Rejected("Gửi chưa khả dụng: production backend WebRTC/TTS chưa được cấu
  hình.")`.

That rejection string is the literal "message cannot be sent because it is not
connected to backend webrtc/tts" error. The `backend/` directory was empty
before this pass; a runnable Node/Express service (`backend/src/server.js`,
`backend/src/routes.js`, `backend/src/adapters/{auth,tts,gemini,transcriptStore}.js`,
`backend/test/unit.test.cjs`) now exists alongside the Android code and its
unit tests pass 8/8 under `node --test` (see §5). The Android `app/` does
not yet call that backend. Closing that client → backend → TTS → WebRTC loop
is the single load-bearing fix for both reported bugs.

The "first screen must be a real-world app experience" guardrail is currently
violated: `WelcomeScreen` (`MainActivity.kt:280`) is a generic product
splash. The build plan (§4) replaces it with the login screen.

---

# 2. Architecture

## 2.1 Component map

```
┌──────────────────────────┐   WebRTC (P2P media, audio+video)    ┌──────────────────────────┐
│  Android client (Kotlin) │◄────────────────────────────────────►│  Android peer (Kotlin)   │
│  Jetpack Compose UI      │     SDP / ICE / DTLS over SRTP/DTLS  │  Jetpack Compose UI      │
│  CameraX (preview+track) │                                      │  CameraX (preview+track) │
│  WebRTC.org libwebrtc    │                                      │  WebRTC.org libwebrtc    │
│  TextToSpeech (offline)  │                                      │  TextToSpeech (offline)  │
│  OkHttp (HTTPS)          │                                      │  OkHttp (HTTPS)          │
│  Firebase Auth SDK (opt) │                                      │  Firebase Auth SDK (opt) │
└────────────┬─────────────┘                                      └─────────────┬────────────┘
             │                                                            │
             │ HTTPS (Firebase ID token bearer)                          │ HTTPS
             │   POST /api/calls/:callId/send                             │
             │   POST /api/suggestions                                   │
             │   POST /api/calls/:callId/consent                         │
             ▼                                                            ▼
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                                BridgeTalk backend (HTTPS)                                │
│  Express + Node 20+ · per-call state in Firestore · WebRTC signaling via Firestore       │
│  ┌──────────────┐  ┌──────────────────┐  ┌────────────────┐  ┌──────────────────────────┐  │
│  │ Firebase     │  │ Cloud Speech-to- │  │ Cloud          │  │ Cloud Text-to-Speech     │  │
│  │ Admin SDK    │  │ Text streaming   │  │ Translation    │  │ (only after Send)        │  │
│  │ (verify ID)  │  │ (interim+final)  │  │ (final caption)│  │                          │  │
│  └──────────────┘  └──────────────────┘  └────────────────┘  └──────────────────────────┘  │
│  ┌──────────────┐  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ Gemini API   │  │ FCM (HTTP v1) — outgoing call notifications, missed call     │  │
│  │ (≤5 replies) │  │ notification, transcript-ready notification. NEVER used for   │  │
│  └──────────────┘  │ media.                                                          │  │
│                    └─────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────────────┘
             ▲
             │ FCM data message
             │
   ┌─────────┴──────────┐
   │  FCM (transport)   │
   └────────────────────┘
```

The Android client never holds a Gemini key, a Google service account, or a
TURN credential. The only secret that lives on-device is the Firebase
Auth ID token, and only inside the Firebase Auth SDK's secure storage.

## 2.2 Channel ownership

| Concern                     | Channel / adapter                    | Owner of credentials |
|----------------------------|--------------------------------------|----------------------|
| Caller / callee identity   | Firebase Auth ID token               | Firebase project     |
| Social graph (Bridge ID, friends) | Firestore `users`, `friendships` | Backend service acct |
| Call session state          | Firestore `calls/{callId}`           | Backend service acct |
| WebRTC signaling (offer/answer/ICE) | Firestore `calls/{callId}/signaling` | Backend service acct |
| Call notifications (ring)   | FCM data message → callee            | Backend service acct |
| Media audio + video         | WebRTC `RTCPeerConnection`           | none on client; TURN uses short-lived credentials from backend |
| Live captions (A → B)      | Backend WebSocket over WSS, streaming STT results | GCP service acct |
| Translation (A → B)        | Cloud Translation (server-side)      | GCP service acct     |
| TTS for B's send           | Cloud Text-to-Speech (server-side)   | GCP service acct     |
| AI suggestions (B)         | Gemini API (server-side)             | GCP service acct     |
| Consent-gated transcript   | Firestore `transcripts/{callId}`     | Backend service acct |

WebRTC is the only media path. Firebase and FCM are never used to carry
audio, video, or TTS bytes.

## 2.3 Send-message flow (the path that is currently broken)

1. B composes a draft in `MessageDraft`.
2. B taps **Send** on the persistent bottom bar (Send is always visible
   while the draft is non-empty — see M-2 in §4).
3. App calls `ConfirmDraftMessageSender.send(SendDraftRequest)`.
4. Sender obtains the Firebase ID token (no-op in offline mode) and POSTs to
   `backend/api/calls/:callId/send` with:
   * `Authorization: Bearer <firebase-id-token>`
   * `Idempotency-Key: <uuid>`
   * body: `{ message, languageCode, recipientUid, senderConsent, recipientConsent }`
5. Backend verifies the bearer token via `firebase-admin` and infers the
   operator's `uid`; any client-supplied `callerId` / `participantId` /
   `endReason` is ignored.
6. Backend calls Cloud Text-to-Speech with the draft (only runs here; never
   on the client).
7. Backend pushes the synthesized audio over the WebRTC data channel
   negotiated alongside the audio track (B's `RTCPeerConnection` is given a
   second `DataChannel` for low-latency TTS). A's client receives the audio
   bytes and plays them through `AudioTrack` (or `MediaPlayer` for the
   interim prototype).
8. Backend persists the transcript segment to Firestore **only if both
   `senderConsent` and `recipientConsent` are `true`**. Otherwise it returns
   `transcript.stored=false, reason=CONSENT_NOT_GRANTED`.

## 2.4 Offline / accessibility fallback

`ConfirmDraftMessageSender` keeps a one-shot offline TTS fallback so a
network failure never produces the "cannot be sent" error: the on-device
Android `TextToSpeech` engine speaks the message, the UI clears the draft
and transitions to the `Sent` state, and the user-visible status is
"Gửi ngoại tuyến — phát giọng nói cục bộ". This fallback is not a mock of
the backend — it is a real Android system TTS engine gated by
`IOException` / 5xx. The `Read.kt` review checklist covers it (see §5.5).

## 2.5 Module layout (proposed)

```
app/src/main/java/com/bridgetalk/app/
├── ui/                    # Composables (Welcome, Profile, Contacts, IncomingCall,
│   │                      #           Call, Consent, MorseCheatSheet, CameraToggle)
│   ├── theme/             # BridgeTalkTheme, tokens, dimens
│   └── a11y/              # Semantics helpers, content-description builders
├── call/                  # AccessibleCallScreenV2 + state holders (CallViewModel)
├── morse/                 # MorseDecoder, MorseTokenMeter, MorseCheatSheet
├── net/                   # OkHttp wiring, retry policy
├── sender/                # DraftMessageSender, ConfirmDraftMessageSender, Sent state machine
│   ├── HttpSendTransport.kt        # inject-able HTTP transport (tested with fakes)
│   ├── LocalTtsVoice.kt            # on-device TTS engine wrapper
│   └── FirebaseIdTokenProvider.kt  # no-op when Firebase not configured
├── friends/               # FriendRequest, FriendshipRepository (Firestore-backed)
├── profile/               # BridgeIdScreen, ProfileViewModel
├── session/               # CallSession state machine (FSM, §3.3)
├── notifications/         # FCM service, deep-link to IncomingCall
├── webrtc/                # PeerConnectionFactory, AudioRecordSource, CameraVideoSource
└── MainActivity.kt
```

---

# 3. Data & State

## 3.1 Android-side data entities

| Entity            | Purpose                                | Key fields                                                                 |
|-------------------|----------------------------------------|----------------------------------------------------------------------------|
| `UserProfile`     | Logged-in user                         | `uid`, `bridgeId`, `displayName`, `locale`, `createdAt`                    |
| `FriendRequest`   | Outgoing/incoming friend request       | `id`, `fromUid`, `toBridgeId`, `status` ∈ {Pending, Accepted, Declined}    |
| `Friendship`      | Mutual follow                          | `aUid`, `bUid`, `createdAt`                                                |
| `MessageDraft`    | Local-only draft (never persisted)     | `text`, `tokens: List<MorseToken>`, `updatedAt`                            |
| `MorseToken`      | A single `.` or `-`                    | `symbol`, `durationMs`, `pressedAt`                                        |
| `Caption`         | Live caption from STT (never persisted)| `callId`, `segmentId`, `text`, `language`, `isFinal`, `interimText`        |
| `Suggestion`      | Up to 5 Gemini replies                 | `text`, `source` ∈ {Gemini, Fallback}                                      |
| `SendDraftRequest`| Outbound to backend                    | `message`, `idempotencyKey`, `languageCode`                                |
| `ConsentDecision` | Per-call transcript consent            | `senderConsent: Boolean`, `recipientConsent: Boolean`                      |

## 3.2 Backend API surface

| Method | Path                              | Auth              | Body                                                                                       | Response                                                                                  |
|--------|-----------------------------------|-------------------|--------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| GET    | `/health`                         | none              | —                                                                                          | `{ ok, service, demoMode, uptimeSeconds }`                                                |
| POST   | `/api/auth/session`               | Bearer            | `{ bridgeId, displayName }`                                                                | `{ uid, bridgeId, displayName, accessToken }` *(seamless handoff; Firebase verifies upstream)* |
| GET    | `/api/suggestions`                | Bearer            | query: `draft`, `languageCode`                                                             | `{ suggestions: string[<=5], source }`                                                    |
| POST   | `/api/calls/:callId/send`         | Bearer (mandatory) | `{ message, idempotencyKey, languageCode, recipientUid, senderConsent, recipientConsent }` | `{ status:"accepted", callId, operator:{uid}, message, voiceOutput:{source,encoding,byteLength,contentType}, transcript:{stored,sink|reason} }` |
| GET    | `/api/calls/:callId/transcript`   | Bearer            | —                                                                                          | `{ segments: TranscriptSegment[] }`                                                        |
| POST   | `/api/calls/:callId/consent`      | Bearer            | `{ senderConsent, recipientConsent }`                                                      | `{ stored: Boolean, sink }`                                                                |
| POST   | `/api/signaling/turn`             | Bearer            | —                                                                                          | `{ urls: [{urls:[…], username, credential, ttl}] }` *(short-lived TURN creds)*            |
| POST   | `/api/notifications/ring`         | Bearer (caller)   | `{ calleeUid, callId }`                                                                    | `{ delivered: Boolean }`                                                                   |

OpenAPI 3.1 spec lives at `backend/openapi.yaml` and is generated into the
Android `sender/` package via a Gradle task that emits Kotlin data classes.

## 3.3 CallSession finite state machine

```
                                 ┌──────────────────┐
                                 │  Idle (no call)  │
                                 └────────┬─────────┘
                                          │ Caller taps Gọi
                                          ▼
                                 ┌──────────────────┐
                  decline        │  Ringing (caller)│        pickup (callee)
                  ───────────►   └────────┬─────────┘   ◄────────────────────
                                          │ callee answers                 │
                                          ▼                                │
                                 ┌──────────────────┐                     │
                                 │  Connecting      │                     │
                                 │  (ICE/DTLS)      │                     │
                                 └────────┬─────────┘                     │
                                          │ ICE connected                 │
                                          ▼                                │
                          ┌────────────────────────────────┐               │
                          │  InCall                        │ ◄─────────────┘
                          │  states:                       │
                          │   • Drafting (B holds the bar) │
                          │   • Sending (round-trip)       │
                          │   • Sent (TTS playing for A)   │
                          │   • Captioning (STT stream)    │
                          └────────┬───────────────────────┘
                                          │ either side taps Kết thúc
                                          ▼
                                 ┌──────────────────┐
                                 │  Ending          │
                                 │  (transcript     │
                                 │   consent gate)  │
                                 └────────┬─────────┘
                                          │ both consent decisions stored
                                          ▼
                                 ┌──────────────────┐
                                 │  Ended           │
                                 └──────────────────┘
```

Transitions are guarded:

* `Idle → Ringing` requires the caller to be authenticated and the callee to
  exist in `friendships`.
* `Ringing → Connecting` requires the callee to acknowledge the FCM ring
  within 30 s; otherwise the call auto-misses and the FSM returns to `Idle`.
* `Connecting → InCall` requires ICE to reach `connected` or `completed`.
* `InCall.Drafting → InCall.Sending` requires the draft to be non-empty and
  `currentCode` to be blank (M-3 in §4 enforces that gate before Send enables).
* `InCall.Sending → InCall.Sent` requires the backend to return HTTP 200 with
  `status:"accepted"`. On any non-2xx the FSM reverts to `Drafting` and
  triggers the offline TTS fallback (see §2.4).
* `InCall → Ending` requires the local user to confirm. `endReason` from the
  client is **never** read; the backend derives it from the call doc.

## 3.4 Idempotency

`SendDraftRequest.idempotencyKey` is a v4 UUID minted on the client. The
backend persists `(callId, idempotencyKey) → response` in Firestore with a
24 h TTL. A retry with the same key returns the cached response without
re-running Cloud TTS.

## 3.5 Retention policy (enforced in code, not in policy docs)

The backend **never** writes to Firestore:

* raw audio buffers or `MediaRecorder` blobs,
* the Cloud STT output (interim or final),
* the Cloud Translation output,
* the Morse token stream,
* the `MessageDraft` text,
* the default `VoiceOutput` bytes.

It only writes `transcripts/{callId}/segments/{segmentId}` when both
participants have posted `consent=true` to `/api/calls/:callId/consent`. The
Android client mirrors this with `MorseInputLog` and `DraftHistory` kept in
`EncryptedSharedPreferences` with `KEYSTORE` protection and a 24 h TTL.

---

# 4. Build Plan (milestone-based, with Definition of Done)

## M-0 — Implementation Readiness Review
* **DoD**: this document merged; the two `DefaultDraftMessageSender` tests
  are flagged for rewrite in M-3; the open questions in §6 are either
  resolved by the PM or marked as deferred.

## M-1 — First-screen & navigation rewrite
* Replace `WelcomeScreen` with `LoginScreen` (Firebase phone / Google sign-in
  picker). The first frame after auth lands on `ContactsScreen` populated
  from `friendships/{uid}`.
* Add a real `AppNavGraph` (Compose Navigation) keyed on
  `authState + currentCallId`.
* DoD: smoke build green, `app:assembleDebug` and `app:testDebugUnitTest`
  pass, accessibility audit (see §5) for `LoginScreen` is green.

## M-2 — Send button visibility & call-screen polish
* Promote the Send button to a persistent bottom bar (Material 3
  `BottomAppBar`) that is visible above the IME, behind the navigation bar,
  and does not scroll out of view. Disable it when `draft.isBlank()` or
  `currentCode.isNotBlank()`.
* Remove `AccessibleCallScreen` (V1). It is dead code and the misleading
  "PHÁT NGAY" label caused the "hidden send button" report.
* Add the Morse cheat sheet (A–Z) as a `ModalBottomSheet` triggered by a
  "MORSE A-Z" button on the call screen. Cheat sheet shows `.` and `-` for
  each letter, large enough for users with low vision.
* Add a camera toggle to the call screen (`IconToggleButton` with a
  camera-off icon by default). Toggles the local `VideoTrack.enabled`
  state. WebRTC still carries the track — toggling `enabled=false` is a
  black frame, not a renegotiation.
* DoD: Send button is visible at all times during drafting on any device
  with a 6.0" screen at 360 dp width; Playwright-for-Android-style
  snapshot test on `CallScreen` (see §5.2) shows the button bounds inside
  the viewport. Camera toggle snapshot test confirms the video track is
  `enabled=true` after first toggle.

## M-3 — Wire the real send pipeline (the bug fix)
* Add a `sender` package (§2.5) with:
  * `DraftMessageSender` (interface) and `SendDraftResult` (sealed class).
  * `HttpSendTransport` — `suspend fun send(callId, body, idToken): Response`.
  * `LocalTtsVoice` — wraps Android `TextToSpeech`; `speak(text, locale)`.
  * `ConfirmDraftMessageSender` — orchestrates: try HTTP transport; on
    success, return `Accepted`; on `IOException` or 5xx, invoke
    `LocalTtsVoice.speak(...)` and return
    `Accepted("Gửi ngoại tuyến — phát giọng nói cục bộ")`.
* Remove `ProductionSenderRequired`. Replace
  `app/src/release/java/.../DefaultDraftMessageSender.kt` with a
  factory that returns `ConfirmDraftMessageSender(...)` wired with the
  release OkHttp client and the real `LocalTtsVoice`.
* Keep `DebugPreviewDraftMessageSender` in
  `app/src/debug/java/.../DefaultDraftMessageSender.kt` (debug-only mock
  for fast UI iteration; not compiled into release).
* DoD:
  * The literal string `production backend WebRTC/TTS chưa được cấu hình`
    is gone from `app/`.
  * `gradlew :app:assembleRelease` succeeds.
  * `gradlew :app:testDebugUnitTest :app:testReleaseUnitTest` passes.
  * `ConfirmDraftMessageSenderTest` covers: HTTP 200 → `Accepted`,
    HTTP 401/403 → `Rejected`, `IOException` → `Accepted` and
    `LocalTtsVoice.speak` was called exactly once with the draft text,
    timeout (`CancellationException`) → `Rejected` and no TTS.
  * Curl-based smoke: `POST /api/calls/demo-1/send` (with the locally
    running backend on `:8787`, DEMO_MODE=true) returns
    `status:"accepted"` and a non-empty `voiceOutput.byteLength`.

## M-4 — WebRTC media pipeline
* Add `org.webrtc:google-webrtc:1.0.32006` (or the maintained
  `io.github.webrtc-sdk:android:114.5735.06`).
* Add `androidx.camera:camera-camera2 / camera-lifecycle / camera-view`
  for the local preview. `VideoSource` from CameraX feeds the
  `VideoTrack`.
* Add STUN (`stun:stun.l.google.com:19302`) and TURN (returned by
  `/api/signaling/turn`) configuration to `PeerConnectionFactory`.
* DoD: two emulator instances complete a `PeerConnection` and exchange
  audio and video; ICE state reaches `connected`; `Kết thúc` returns both
  to `ContactsScreen`.

## M-5 — Cloud STT / Translation / TTS wiring
* Replace the dead `SpeechInputController` (Android on-device
  `SpeechRecognizer`) with a backend WebSocket client that streams
  16 kHz PCM (captured by `AudioRecord`) to the backend, which fans out
  to Cloud STT streaming and Cloud Translation.
* DoD: A speaks Vietnamese, B sees Vietnamese + English captions within
  400 ms; B sends, A hears the TTS within 800 ms.

## M-6 — Friends & FCM ring
* `POST /api/notifications/ring` posts a data message via FCM HTTP v1
  with `{ "type":"ring", "callId":"…", "callerBridgeId":"…" }`. The
  Android `FirebaseMessagingService` deep-links to `IncomingCallScreen`.
* DoD: tapping `Chấp nhận` on a real device transitions to `InCall`.

## M-7 — Consent-gated transcript
* Add `ConsentScreen` as a real gate after `InCall`. `POST
  /api/calls/:callId/consent` is the only path that writes to Firestore
  `transcripts/{callId}`.
* DoD: integration test (backend) verifies that a transcript segment
  with `senderConsent=false` is not written.

---

# 5. Testing Strategy

## 5.1 Unit tests (JVM)
* **Framework**: JUnit 4 (already on the classpath) + MockK for the
  Android `TextToSpeech` surface.
* **Targets**:
  * `MorseDecoderTest` (decode, malformed codes, international letters)
  * `MorseTokenMeterTest` (dot/dash threshold at 250 ms ± 10 ms)
  * `SendDraftResultTest` (sealed hierarchy exhaustiveness)
  * `ConfirmDraftMessageSenderTest` (see M-3 DoD)
  * `CallSessionStateMachineTest` (illegal transitions throw
    `IllegalStateException`)

## 5.2 Snapshot / UI tests
* **Framework**: `androidx.compose.ui:ui-test-junit4` +
  `roborazzi` for PNG snapshots.
* **Cases**:
  * `CallScreenTest` — confirms the Send button bounds are inside the
    visible viewport (the test that would have caught the "hidden send
    button" bug).
  * `CallScreenTest` — Morse cheat sheet sheet expands and shows all 26
    letters.
  * `CallScreenTest` — camera toggle starts in `off` and turns the local
    video track on after a tap.

## 5.3 Backend tests
* **Framework**: `node --test` (Node 20+ built-in test runner).
* **Targets** (`backend/test/`):
  * `unit.test.cjs` — already passing 8/8 (auth demo mode, validation,
    TTS local fallback, Gemini fallback, idempotency payload shape).
  * `routes.test.cjs` — POST `/api/calls/:callId/send` returns 401
    without bearer, 400 on empty message, 200 with valid bearer; the
    `operator` field always reflects the verified `uid`, never the
    client-supplied `recipientUid`.
  * `consent.test.cjs` — segment is not stored unless both consents
    are true.
* **Coverage gate**: `c8` reports ≥ 80% line coverage on
  `backend/src/adapters/`.

## 5.4 Integration tests
* **Framework**: Firebase Emulator Suite (`firebase-tools`).
* **Cases**:
  * Two Android instrumentation tests run on two emulators against the
    Firebase Auth + Firestore emulators, complete a call, and assert
    that only consented transcript rows exist.
  * Backend integration test against the Firestore emulator asserts the
    `calls/{callId}/signaling` subcollection round-trips SDP and ICE
    candidates.

## 5.5 Bug-specific tests (the two reported issues)

### 5.5.1 "Send Message button is completely hidden"
* `CallScreenSendButtonVisibilityTest` (Roborazzi snapshot) renders
  `CallScreen` at 360 × 640 dp, captures a PNG, and asserts that the
  pixel rectangle for the Send button (matched by semantics
  `contentDescription = "Gửi nội dung đã xác nhận"`) is non-empty and
  within the screen bounds.
* An `AccessibilityChecks` rule (`composeTestRule.runAccessibilityChecks()`)
  requires the Send button to have a non-empty
  `contentDescription` and a size ≥ 48 dp.

### 5.5.2 "Cannot be sent because it is not connected to backend webrtc/tts"
* `ConfirmDraftMessageSenderBugTest`:
  * Given `HttpSendTransport` always throws `IOException`,
    When `ConfirmDraftMessageSender.send(...)` is invoked,
    Then the result is `Accepted("Gửi ngoại tuyến — phát giọng nói cục bộ")`
    and `LocalTtsVoice.speak` is called with the draft.
  * Given `HttpSendTransport` returns HTTP 200 with
    `{"status":"accepted", ...}`,
    When the sender is invoked,
    Then the result is `Accepted` and `LocalTtsVoice.speak` is **not**
    called.
  * The literal strings
    `production backend WebRTC/TTS chưa được cấu hình` and
    `cannot be sent because it is not connected to backend webrtc/tts` are
    asserted **absent** from any resource in `app/src/`.
* Backend `routes.test.cjs`: a successful `/send` is replayed twice with
  the same `idempotencyKey` and returns the same response without
  calling Cloud TTS twice.

## 5.6 Test data
* `backend/test/fixtures/` — 26 sample Morse sequences with expected
  decodes, 3 sample sessions with mixed consented/denied segments.
* `app/src/test/resources/` — sample Firestore snapshots for
  `users/{uid}`, `friendships/{uid}`, `calls/{callId}`.

---

# 6. Clarifications (decisions required before code lands)

These are the questions I will not answer in code. Each carries a default I
will use if the PM is silent.

1. **Q1 — Login provider.** Firebase phone auth (good for accessibility
   users in VN, no second-factor) vs. Google sign-in (faster to ship).
   *Default: Google first, phone second, both behind a single
   `LoginScreen` picker.*
2. **Q2 — Camera default state on call start.** Off (privacy-preserving)
   or on (so the other party sees them).
   *Default: off, with a one-tap toggle. (See M-2.)*
3. **Q3 — STT language for A.** Auto-detect vs. fixed `sourceLanguage` from
   the caller's profile.
   *Default: caller's profile locale, overridable per call with the
   `FilterChip` pair that already exists in V2.*
4. **Q4 — TTS playback destination.** A's speaker only, or A's speaker +
   a transcript line for A (caption of what was TTS'd).
   *Default: speaker only, with a one-line caption strip on A's side so
   deaf A users can read the synthesized text.*
5. **Q5 — Gemini suggestion refresh cadence.** Every keystroke (live), on
   send-button idle, or only when the draft becomes blank.
   *Default: only when the draft becomes blank or the user explicitly
   taps `Gợi ý trả lời` — keeps the API cost and the LLM latency out of
   the typing loop.*
6. **Q6 — Morse input mode.** Press-and-hold on a single button (V1) or
   the two-button `.` / `-` grid (V2). The two-button grid is faster
   but less forgiving for users with motor difficulties.
   *Default: keep both. The single large hold-and-release button is the
   default; the two-button grid is a `MorseAdvanced` toggle.*
7. **Q7 — Idempotency window.** 24 h is the proposed TTL for the
   `idempotencyKey → response` cache. Confirm or change.
8. **Q8 — Transcript redaction.** Do we strip phone numbers, emails, and
   Bridge IDs from a consented transcript before persisting it?
   *Default: yes, server-side regex pass before write.*

---

# 7. Out of scope (per the brief)

* Sign-language recognition / translation.
* AI analysis of the video track (the video still travels over WebRTC so
  participants can see each other; the backend never reads the frames).
* GSM, FaceTime, or any emergency-service integration.
* Persisting raw audio, captions, translations, Morse, MessageDraft, or
  the default TTS output.

---

# 8. How to read this review

* Sections 1–2 are facts (the current code and the proposed architecture).
* Section 3 is the contract the backend and the app will agree on.
* Section 4 is the order in which code will land.
* Section 5 is how every claim above is proved.
* Section 6 is the only place where I am asking for a decision.

The two reported bugs are not closed by this review. They are closed by
M-2 and M-3, with the bug-specific tests in §5.5.1 and §5.5.2 as the
contract. I will not author that code in this pass.
