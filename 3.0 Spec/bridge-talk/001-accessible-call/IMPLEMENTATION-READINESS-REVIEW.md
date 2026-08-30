---
module: bridge-talk
document: implementation-readiness-review
version: 0.4.0
status: user-review-required
reviewed_on: 2026-08-27
scope: Android MVP production implementation readiness
---

# BridgeTalk Implementation Readiness Review

## 0. Executive verdict

**Decision: NOT READY for production code generation. READY for Milestone M0 (decision closure and contract hardening).**

The repository has a coherent six-file draft SPEC and a usable native Android Kotlin/Jetpack Compose UI prototype. The target boundaries are also clear: WebRTC carries call audio; Firebase Auth supplies identity; Firestore supplies social/session/signaling data; FCM supplies call notifications; Google Cloud services and Gemini are reached through trusted backend adapters.

Production implementation is blocked by unresolved decisions that materially affect security, media topology, APIs, privacy, and testability. The highest-risk open gaps are the Speech-to-Text audio path, injection of TTS audio into WebRTC, TURN and reconnect behavior, and transcript retention/deletion. The following implementation rules are already decided rather than open: the production Morse control is one large press-hold/release button; an accessible Morse `A-Z`/`0-9` reference is available from the call UI; the first screen is a real authentication/home experience rather than a generic marketing landing page; audio and optional standard video use WebRTC; users can toggle their own camera; and every input method places content in the editable `MessageDraft`, with TTS gated by an always-visible explicit Send action.

### Verified repository baseline

| Area | Verified current state | Readiness impact |
|---|---|---|
| Android | Native Kotlin + Jetpack Compose, `minSdk 26`, `targetSdk 35`; one activity and in-memory navigation | Kotlin is confirmed by the official requirement and project configuration; the application structure is still prototype-level. |
| Project root | Root contains `build.gradle.kts` and `settings.gradle.kts`; `settings.gradle.kts` names `BridgeTalk` and includes `:app` | The explicit directory requirement is satisfied. |
| Production dependencies | No Firebase, FCM, WebRTC, Google Cloud, DI, networking, persistence, or coroutine test dependencies are declared | No production integration path exists yet. |
| Current call UI | Static/sample caption and translation; local Android `SpeechRecognizer`; hard-coded suggestions; `ToneGenerator`; no remote peer | Useful interaction prototype only, not a real call implementation. Android `SpeechRecognizer` is not the required Cloud Speech-to-Text streaming adapter. |
| Morse input | Current `AccessibleCallScreenV2` uses two direct buttons for dot and dash | Known implementation nonconformity: production must use the official one-button press-hold/release interaction. |
| Morse reference | No accessible alphabetical Morse cheat sheet is implemented in the active call UI | Required call-time learning/reference aid is missing. Its presentation and TalkBack behavior need an approved UI contract. |
| Camera/video | No camera permission, local/remote video surface, camera state, or WebRTC video track exists | The new camera-toggle and standard WebRTC video requirement is not implemented. Video recognition remains out of scope. |
| First screen | App initializes `AppScreen.Welcome` and shows a product-introduction screen before profile | Known implementation nonconformity: production must route signed-out users to real login and signed-in users to the authenticated home/contact experience. |
| Universal Send | `AccessibleCallScreenV2` contains a `GỬI` button and routes it through `DraftMessageSender`, but the whole call screen scrolls, so the action is not guaranteed to remain visible while drafting. The debug source set accepts through a labeled preview adapter; the release source set returns the reported “production backend WebRTC/TTS is not configured” rejection. | Both reported defects remain open for production: the action must be persistently visible, and success must come from a real authenticated backend/TTS/WebRTC delivery acknowledgement rather than a preview adapter. |
| Consent UI | Prototype only exposes a “Không lưu” completion path | Does not implement two-party `Granted`/`Declined` coordination. |
| SPEC | `business-rules`, `dataflow`, `userflow`, `wireframe`, `usecase`, and `userstory` exist with BR-BT-01..17 | Strong starting point, but all six files remain `draft` and contain open decisions. |
| Build verification | The wrapper is pinned to Gradle 8.9 for AGP 8.7 compatibility. Debug and release Kotlin compilation pass. The two sender contract tests pass when the same checkout is addressed through a temporary ASCII drive mapping. From the original `C:\Ổ D\...` path, Gradle's Windows test worker compiles the tests but throws `ClassNotFoundException` while loading them. | Sender behavior is verified, but the Windows Unicode-path runner defect remains an environment/tooling issue. `gradlew.bat` is also unreliable from this path; CI should use an ASCII checkout path. |
| Git verification | Current workspace is not recognized as a Git worktree | Cleanliness, branch, and change history cannot be asserted here. |

### Verified UI/code findings to fix only after plan approval

1. **P0 real delivery is broken:** the release sender always returns the reported backend/WebRTC/TTS-not-configured error. A real fix requires an authenticated send API, backend Cloud TTS adapter, a defined WebRTC audio-injection topology, delivery/playback acknowledgement, and server-side idempotency. The debug preview adapter is test/demo behavior and is not evidence that a recipient received or heard anything.
2. **P0 Morse contract mismatch:** production requires one large press-hold/release control, while the active V2 screen renders separate `CHẤM` and `GẠCH` buttons.
3. **P1 critical action can be off-screen:** the entire call screen is a single vertical scroll and Send appears after all five suggestion chips. At large font/display scale, the main action can move far below the draft and Morse controls. The approved redesign should keep the shared Send action persistently discoverable without obscuring captions or controls.
4. **P1 large-text layout risk:** language controls and voice input share a fixed horizontal row, while correction actions share another non-wrapping row. These need responsive/flow layouts and instrumentation at the approved font/display scales.
5. **P1 misleading live state:** the caption card always displays sample text and “ĐANG NGHE,” independent of a WebRTC/STT state. Production UI must render explicit negotiating/listening/reconnecting/failed states from real adapters.
6. **P1 required call controls are absent:** no camera toggle/video surfaces or accessible Morse alphabetical reference exist in the active call UI.

Kotlin Send-flow implementation began only after the user explicitly approved the review. This does not close the remaining production-readiness gates or prove real recipient delivery.

### Product-readiness gate

The requirement states a valuable product direction but does not yet define an outcome baseline, target, or time window. Before public MVP scope is frozen, Product should approve measurable success criteria such as successful two-person call completion, caption usability/latency, successful intentional Morse-to-voice sends, and task completion by representative users with speech/hearing/motor-access needs. These must be measured without logging communication content.

**Problem statement (supported by the stated requirement):** people with speech or hearing difficulties, or both, need a controllable way to complete a live 1-on-1 conversation without surrendering authorship of what they say.

**Outcome statement:** incomplete and therefore not approved. The target users and desired behavior are known, but metric, baseline/first-round success criteria, target, and timeframe remain CL-BT-20.

Condensed Product Mindset check:

| Axis | PASS | FAIL / evidence still required |
|---|---|---|
| Outcome | Problem is separated from technology; “features shipped” is not treated as success | No complete outcome statement, value metric, baseline/first-round criterion, target, or timeframe |
| Design | A one-day-scale UI prototype exists | No repository evidence of target-user observation, approved HMW framing, alternatives considered, or structured usability results |
| Critical | This review separates verified current state, target design, and unresolved decisions | Root-cause evidence, bias review, and outcome hypothesis testing are not documented |

These FAIL items are product/research gates, not permission to invent missing evidence.

### Resolved implementation decisions from the official requirement

| ID | Confirmed decision | Consequence for later code |
|---|---|---|
| CL-BT-01 (resolved) | Morse input uses one large button: press, hold, and release; duration determines dot/dash under the approved timing contract. | Replace the prototype's two direct `CHẤM`/`GẠCH` controls for the production flow. Timing values remain open under CL-BT-16. |
| RD-BT-01 | Android implementation language is Kotlin; the current project uses Jetpack Compose. | Do not reopen Flutter/framework selection during implementation. |
| RD-BT-02 | The project directory must contain a root Gradle build/settings file. | Already satisfied by root `build.gradle.kts` and `settings.gradle.kts`; M0 must repair and pin the wrapper workflow so it also runs from this Unicode Windows path. |
| RD-BT-03 | The first screen is a real app experience, not a generic landing page. | Auth-aware startup routes to login when signed out and the real home/contact surface when signed in; do not ship `WelcomeScreen` as a marketing gate. |
| RD-BT-04 | One prominent, visible Send button—localized as `GỬI` in Vietnamese and `SEND` in English—gates every supported draft-input path, including keyboard typing, Morse decoding, an AI suggestion copied into the draft, and any other approved input. | Morse is decoded, not speech-transcribed, into the same editable `MessageDraft`. The button is disabled for an empty/invalid draft. No input may call TTS directly. When B presses Send, the app submits exactly the text currently displayed, shows an honest pending state, and reports success only after the trusted backend accepts the idempotent request that produces TTS for A over WebRTC. |
| RD-BT-05 | The call UI provides an easily accessible Morse alphabetical reference/cheat sheet. | The reference covers the approved International Morse alphabet (`A-Z`, `0-9`), is reachable without ending the call or losing the draft, and has usable TalkBack, large-text, and focus behavior. |
| RD-BT-06 | Users can toggle their own camera on/off during a call; standard video is allowed, video recognition is not. | Camera capture and remote video use WebRTC only. Camera state must be explicit and privacy-safe; no AI/video-analysis adapter is introduced. |
| RD-BT-07 | Send remains prominently visible at all times while the user is drafting. | Use a persistent call-surface action region or equivalent responsive layout; scrolling captions, suggestions, Morse reference, and keyboard/IME must not hide the action. |

## a) Architecture

### Target component architecture

```text
Android A                                       Android B
  Firebase Auth                                   Firebase Auth
  Call UI + audio/video capture                   Accessible call UI + camera control
  WebRTC audio/video tracks <--- STUN/TURN ---->  WebRTC audio/video tracks
        |                                               |
        +------------ Firestore signaling --------------+
        |        SDP offer/answer + ICE only             |
        +---------------- FCM incoming call ------------>|

Both clients -> HTTPS/realtime channel -> Trusted BridgeTalk backend
                                          - verify Firebase ID token
                                          - infer operator/participant/role
                                          - authorize every session mutation
                                          - own provider credentials
                                          - enforce consent and retention
                                          - expose adapter-neutral contracts
                                                      |
                           +--------------------------+-------------------+
                           |              |            |                  |
                    Speech-to-Text   Translation      TTS               Gemini
                    streaming        final text       after Send        <= 5 suggestions
```

The diagram fixes responsibility boundaries, not the unresolved media topology. M0 must decide how A's audio reaches Speech-to-Text and how generated TTS audio becomes part of the WebRTC media heard by A.

### Android client

Recommended module boundaries for implementation:

| Layer/module | Responsibility | Must not do |
|---|---|---|
| `ui` | Compose screens, accessibility semantics, localization, explicit AI/send confirmations | Hold provider credentials or infer server authority. |
| `domain` | Use cases and pure state reducers for auth, social graph, call, caption, Morse draft, consent | Depend directly on Firebase or provider SDK UI types. |
| `data` | Auth token provider, backend API client, Firestore signaling listener, FCM token registration | Treat client-supplied user IDs as authorization. |
| `rtc` | `PeerConnection`, audio/video tracks, camera capture/toggle, media routing, ICE, reconnect, and TTS media injection after approval | Use Firebase as an audio/video path or add video recognition. |
| `accessibility` | Morse timing/decoder/reference, adjustable thresholds, haptics/audio feedback, TalkBack labels, persistent Send affordance | Auto-send from Morse, keyboard, suggestion, or any other input; auto-select an AI suggestion; or auto-play an unconfirmed draft. |

Use unidirectional state flow with lifecycle-aware ViewModels. Keep provider SDKs behind interfaces so production implementations are real while unit tests can use fakes. Development fakes must be impossible to select in a release build.

### Trusted backend

The backend is the authorization and provider boundary. Every authenticated request must:

1. Verify the Firebase ID token.
2. map `firebaseUid` to `UserAccount`;
3. derive the operator and `CallParticipant` from that identity and the target resource;
4. check friendship, session membership, role, and state;
5. reject client-supplied authority fields such as `callerId`, `participantId`, or `endReason`;
6. issue short-lived, least-privilege resources such as TURN credentials or media URLs when needed;
7. emit content-free audit/security events.

The backend runtime, deployment platform, region, protocol for live captions, and API schema format are not selected. Those are M0 decisions. A versioned OpenAPI contract is recommended for command/query APIs; the caption stream needs a separately versioned WebSocket, SSE, or equivalent realtime contract.

### Firestore

Firestore should hold:

- durable social/application state: user profile, Bridge ID index, friend requests/relationships, minimal call history, FCM device registration metadata, and consent records;
- short-lived call coordination: `CallSession`, participant state, SDP offer/answer, and ICE candidates;
- no raw audio and no default persistence of captions, translations, Morse timing, `MessageDraft`, suggestions, or `VoiceOutput`.

Sensitive lifecycle mutations should go through the backend. If clients write signaling documents directly for latency, Firestore Security Rules must restrict writes to authenticated session participants, validate immutable ownership fields, constrain schemas, and enforce TTL cleanup. Firestore is signaling/state storage only, never a media transport.

### WebRTC and signaling

- One authenticated 1-on-1 `RTCPeerConnection` carries all call audio and any enabled standard video seen/heard by the remote participant.
- Firestore exchanges SDP offer/answer and trickled ICE candidates. FCM only wakes/notifies B; Firestore/backend state remains authoritative.
- STUN/TURN configuration must use short-lived credentials. A real TURN path is required in staging and release tests.
- Call business state and media connection state must be separate. A `Connected` business session must not falsely claim usable audio when ICE/media has failed.
- Reconnect, network handover, audio focus, Bluetooth/wired headset, speaker routing, echo cancellation, simultaneous WebRTC playback plus caption processing, and background behavior require explicit contracts.
- TTS output must enter the authenticated WebRTC audio path after B presses Send; playing audio only on B's loudspeaker is not sufficient.
- Camera on/off changes the local outbound WebRTC video track without ending audio, caption, Morse, or draft flows. No frame is sent to an AI/video-recognition service.

### Google Cloud adapters

| Adapter | Input | Output | Mandatory policy |
|---|---|---|---|
| Speech-to-Text streaming | Authenticated audio stream derived from A's call audio | Ordered `Interim`/`Final` caption events | Define stream rotation/reconnect and sequence deduplication; do not persist raw audio. |
| Translation | Final source caption only | Final translated text | Source caption remains visible; translation failure must not end the call. |
| Text-to-Speech | B's exact final draft, regardless of how it was entered, plus selected voice/language | Audio suitable for injection into WebRTC | No request before explicit Send; retry must not accidentally duplicate playback. |
| Gemini | Current draft plus minimal language/context allowed by policy | Zero to five advisory suggestions | Backend only; preserve original draft; nothing preselected or sent automatically. |

Secrets belong in the backend's secret manager/IAM environment. The Android app may contain public Firebase client configuration as required by Firebase, but never Gemini keys, service-account files, Cloud credentials, or long-lived TURN secrets.

## b) Data & State

### Existing API contract

All endpoints below require a verified Firebase bearer token unless explicitly internal. The backend derives the operator; identity fields in examples are resource data, not caller authority.

| Endpoint | Purpose | Readiness note |
|---|---|---|
| `POST /api/profile` | Create/update `UserAccount` | Define Bridge ID normalization, mutation, and conflict semantics. |
| `POST /api/contact-requests` | Send friend request | Body currently identifies the recipient by `bridgeId`; sender is token-derived. |
| `POST /api/contact-requests/{id}/accept` | Accept request | Decline/cancel/block contracts are missing. |
| `POST /api/call-sessions` | Create a pending call | Caller is token-derived; concurrency/busy behavior is missing. |
| `POST /api/call-sessions/{id}/accept` | B accepts call | B is token-derived; expiry/idempotency must be defined. |
| `POST /api/call-sessions/{id}/end` | End/cancel/decline a call | Body is empty; server derives `endReason`. This boundary is correct. |
| `POST /api/call-sessions/{id}/captions` | Internal Speech adapter callback/update | Must not be callable by Android clients. Provider authentication and ordering are unspecified. |
| `POST /api/captions/{id}/translations` | Translate a final caption | Clarify whether client requests this or backend orchestration does. |
| `POST /api/call-sessions/{id}/tap-sequences` | Submit decoded/invalid Morse sequence | Conflicts with the stated non-retention posture unless explicitly ephemeral; sending raw Morse to backend may be unnecessary. |
| `POST /api/message-drafts/{id}/send` | Confirm the final draft from any supported input method and request TTS | Needs an idempotency key and exact-once playback semantics; input provenance must not create separate bypass endpoints. |
| `POST /api/message-drafts/{id}/suggestions` | Request up to five Gemini suggestions | Needs size limits, safety behavior, and short-lived handling. |
| `POST /api/call-sessions/{id}/transcript-consents` | Record participant's latest decision | Participant is token-derived; pending-consent expiry is unspecified. |
| `POST /api/call-sessions/{id}/interaction-metrics` | Submit a closed-list, content-free event | Prefer server-derived events where possible to prevent spoofing. |

Required contracts that are not yet specified include sign-in method/configuration, profile/contact reads, pending-request list, friend list, device/FCM token registration and revocation, call/session reads or listeners, signaling document schema, caption realtime delivery, TURN credential issuance, transcript retrieval/deletion, and account deletion. Their exact endpoints must be designed in M0 rather than inferred during coding.

### Required API surface to formalize in M0

These are logical contracts, not approved endpoint paths or payload schemas. M0 must version them and decide HTTPS versus Firestore/realtime transport without weakening backend authority.

| Logical API | Caller/transport | Required outcome |
|---|---|---|
| Auth/session bootstrap | Android -> Firebase Auth + backend | Verify ID token and return only token-derived operator/session context. |
| Profile and Bridge ID | Android -> backend | Read/update profile; enforce normalized unique Bridge ID without enumeration leakage. |
| Social graph | Android -> backend | List/send/accept/decline/cancel/remove/block under approved relationship rules. |
| Device registration | Android -> backend | Register/rotate/revoke FCM token without accepting another user's identity. |
| Call commands | Android -> backend | Create/accept/decline/end; server derives actor, participant, transition, and `endReason`. |
| Call state subscription | Android <- Firestore/backend | Observe authorized `CallSession` and participant/media readiness state. |
| WebRTC signaling | Android <-> Firestore under Rules or trusted backend | Exchange scoped SDP/ICE only; expire documents; never carry media. |
| TURN credentials | Android -> backend | Issue short-lived session/user-scoped ICE server credentials. |
| Caption stream | Backend -> authorized Android B | Deliver ordered interim/final source events and final-only translations with reconnect/deduplication. |
| Suggestion command | Android B -> backend -> Gemini | Return zero to five advisory suggestions; do not mutate or send the draft. |
| Confirmed message send | Android B -> backend -> Cloud TTS/WebRTC bridge | Accept exact visible draft plus idempotency key; authorize B/session; synthesize only after Send; inject once into A's WebRTC audio. |
| Playback acknowledgement | WebRTC media bridge/A client -> backend -> Android B | Distinguish accepted/queued/injected/played/failed without trusting client-supplied participant identity. Exact authority is CL-BT-21. |
| Transcript decision/access | Android -> backend | Record token-derived `Granted`/`Declined`; atomically save only after both grants; retrieve/delete only as approved. |
| Account lifecycle | Android -> backend/Firebase Auth | Reauthenticate, revoke devices, delete account, and purge/minimize associated data per policy. |

### Data entities

| Entity | Current states/key fields | Intended storage class |
|---|---|---|
| `UserAccount` | `firebaseUid`, unique `bridgeId`, `displayName` | Durable. |
| `ContactRequest` | `Pending`, `Accepted`, `Declined` | Durable/minimized. A normalized friendship model is still needed. |
| `CallSession` | `Pending`, `Connected`, `Ended`; server-derived `endReason` | Durable minimal metadata plus ephemeral signaling. |
| `CallParticipant` | `Caller_A`, `Receiver_B`, acceptance, language and Morse preference | Minimal session metadata; clarify whether preferences belong on profile. |
| `MediaState` | ICE/connection state plus local audio, local camera, remote audio, and remote video availability | Runtime/ephemeral; only minimal operational state may be retained if approved. |
| `Caption` | `Interim`, `Final`, `Failed` | Memory/ephemeral by default; transcript copy only after bilateral consent. |
| `CaptionTranslation` | `Completed`, `Failed` | Memory/ephemeral by default. |
| `TapSequence` | `Collecting`, `Decoded`, `Invalid` | Prefer client memory only unless a backend need is approved. Never persist timing by default. |
| `MessageDraft` | `Composing`, `Ready`, `Sent`, `Invalid`; current SPEC `sourceType` only permits `Morse` or `QuickPhrase` | Memory/ephemeral by default. M0 must make provenance capable of representing typed, Morse, suggestion-derived, quick-phrase, or mixed edits without changing the universal Send rule; exact schema is not invented here. |
| `MessageSuggestion` | `Available`, `Selected`, `Dismissed` | Memory/ephemeral by default; maximum five. |
| `MorseReference` | Approved static mapping for `A-Z`, `0-9`, display/filter/focus state | Packaged non-user content plus UI state; never contains communication content. |
| `VoiceOutput` | `Queued`, `Playing`, `Completed`, `Failed` | Ephemeral control state/audio; no default retention. |
| `ConsentRecord` | `SaveTranscript`: `Granted` or `Declined` | Durable evidence subject to policy. |
| `InteractionMetric` | Closed event-name list, no communication content | Durable only with the approved analytics basis/consent. |

The current data model does not define a persisted `Transcript` aggregate. M0 must define exactly which final textual items are included, where the encrypted transcript is stored, how it is associated with both grants, who can read/delete/export it, and its TTL. Raw audio, Morse timing/raw sequence, drafts, suggestions, translations, and generated audio must not silently become transcript fields.

### State machines

#### Authoritative `CallSession` business state (current contract)

```text
Create by authenticated A
        |
        v
     Pending ---------------------> Ended(Cancelled)   [A cancels]
        |  \----------------------> Ended(Declined)    [B declines]
        |   \---------------------> Ended(TimedOut)    [server timeout]
        |
        | B accepts while invitation is valid
        v
    Connected --------------------> Ended(ParticipantEnded)
```

Only the server performs transitions. A is accepted at creation; B acceptance moves the session to `Connected`. Transitions must be atomic and idempotent. Terminal state cannot reopen.

#### Required media substate (contract to approve)

```text
Idle -> Negotiating -> CheckingICE -> MediaAvailable
                    \-> Failed
MediaAvailable -> Reconnecting -> MediaAvailable
                              \-> Failed
Any nonterminal state -> Closed
```

This substate prevents the product from equating a business `Connected` session with functioning audio. Timeout values, retry budget, network handover behavior, and whether media failure ends the business session remain open.

#### Local camera/video state

```text
PermissionUnknown -> PermissionGranted -> CameraOff <-> CameraStarting -> CameraOn
        |                    |                 |              |              |
        v                    v                 +-----------> Failed <---------+
PermissionDenied       CameraUnavailable

Any state -> CallEnded
```

Audio/captions/Morse/drafting remain usable when the camera is off, denied, unavailable, or fails. The UI must distinguish “my camera is off” from “remote video is unavailable.” A camera toggle changes only the operator's local outbound video track; it does not authorize video analysis or storage.

#### Caption pipeline

```text
Listening -> Interim(seq) -> Final(seq) -> TranslationPending -> Displayed
    |             |              |                 |
    +-----------> Failed <-------+-----------------+
```

Final events require monotonically ordered sequence numbers and deduplication after reconnect. Translation is only triggered by a final caption and never replaces the source text.

#### B's message and TTS pipeline

```text
Keyboard / Morse / approved input -> Composing -> Ready
                                          |
                                          +-> [optional Suggesting -> suggestion copied to draft]
    |          |
 Invalid <-----+
               |
          B presses Send
               v
             Sent -> TTS Queued -> Playing -> Completed
                                  \-> Failed -> explicit Retry
```

Every input method edits the same visible `MessageDraft` and converges on the same Send action. No AI result changes `Composing/Ready` without B selecting it. No keyboard, Morse, quick phrase, suggestion, or other input may trigger TTS before Send. Send/retry requires idempotency so a network retry cannot speak twice.

#### Send delivery acknowledgement

```text
Ready -> Submitting -> BackendAccepted -> TTSQueued -> InjectingToWebRTC -> PlayedForA
             |               |               |               |
             +-------------> Failed <--------+---------------+
                                  |
                          RetrySameIdempotencyKey
```

`BackendAccepted` alone is not “heard by A.” The UI must use distinct, accessible states for submitted, accepted/processing, played, failed, and retrying. The backend owns deduplication; a reconnect or repeated tap with the same idempotency key must not synthesize or inject a second playback.

#### Transcript consent

```text
NotAsked -> AwaitingDecisions
               | both Granted before expiry -> Saved
               | either Declined             -> Purged
               | missing/expired decision    -> Purged
```

Ephemeral content must have an enforced short TTL while decisions are pending. “No response” is not consent. Saving should be an atomic server decision tied to both latest `Granted` records.

## c) Clarifications

The following questions require explicit decisions. No answer should be inferred during implementation.

### P0 — blocks production architecture or security

| ID | Decision required | Why it blocks |
|---|---|---|
| CL-BT-02 | How is A's audio copied from WebRTC capture to backend Cloud Speech-to-Text: client-side authenticated fork, server media relay/SFU, or another approved topology? | Determines privacy notice, latency, Android audio implementation, infrastructure, and cost. |
| CL-BT-03 | How is Cloud TTS output injected into B's outbound WebRTC audio track so A hears it? How are echo, mixing, ducking, cancellation, and duplicate retry handled? | A local playback shortcut violates the media constraint. |
| CL-BT-04 | Which TURN provider/deployment, regions, credential service, TTL, and fallback policy are approved? | Calls will fail on restrictive NAT/mobile networks without a production relay path. |
| CL-BT-05 | Which backend runtime/platform, deployment region, data region, and realtime caption protocol are approved? | Affects API, streaming, IAM, latency, and operational ownership. |
| CL-BT-06 | Which Firebase Auth methods are in MVP, and what are account recovery, account deletion, reauthentication, and age/eligibility rules? | The first-run and security flows cannot be completed from “Firebase Auth” alone. |
| CL-BT-07 | What exact notice/consent or other approved legal basis is required before audio/text is processed by Speech, Translation, TTS, Gemini, and analytics? What happens if declined? | Transcript consent after a call is distinct from permission to process data during a call. |
| CL-BT-08 | What is a saved transcript, where is it stored, who can access/export/delete it, and what is its retention period? How long may ephemeral content wait for the second decision? | Current policy says when saving is allowed but not how retention and deletion work. |
| CL-BT-09 | Are Caller A/Receiver B permanently tied to speaking/non-speaking roles and call direction? Can both users need captions/Morse, or can roles switch in a call? | Changes participant model, audio/caption directions, screens, and accessibility behavior. |
| CL-BT-10 | What are invitation expiry, busy/concurrent-call policy, duplicate accept/end idempotency, reconnect timeout, and behavior after app/process death? | The current three-state session model does not define these production edge cases. |
| CL-BT-21 | Which approved media topology injects backend-generated TTS into B's outbound WebRTC audio while preserving B's microphone/audio controls, and what event proves A playback rather than mere request acceptance? | This is the direct blocker behind the reported send error and determines whether exact-once delivery can be claimed. |

### P1 — blocks feature completeness or acceptance tests

| ID | Decision required |
|---|---|
| CL-BT-11 | Bridge ID character set, case normalization, length, uniqueness, change policy, enumeration protection, and whether search reveals display names before acceptance. |
| CL-BT-12 | Friend-request decline, cancel, remove friend, block/report, abuse/rate-limit, and calling permission after relationship changes. |
| CL-BT-13 | Manual versus automatic source-language selection; supported `vi-VN`/English locales; behavior for code-switching and unsupported speech. |
| CL-BT-14 | Approved voices, speaking rate, output language ownership, maximum draft length, profanity/safety behavior, and whether B previews TTS privately before Send. |
| CL-BT-15 | Gemini model/hosting choice, prompt/context limits, safety settings, latency timeout, quotas, and whether a suggestion can be requested from an empty draft. |
| CL-BT-16 | Morse alphabet and separators, adjustable min/max press durations, character/word timeout, motor-access presets, correction/undo, and Vietnamese input strategy. |
| CL-BT-17 | FCM incoming-call UX in foreground/background/killed states, notification permission denial, ring duration, ringtone/vibration, and Android full-screen notification policy. |
| CL-BT-18 | Audio routing and controls: speaker/earpiece/Bluetooth, mute, audio focus, interruption by another app/call, and whether B always hears original audio while TTS/captions operate. |
| CL-BT-19 | Accessibility acceptance targets: font scale, minimum touch target, TalkBack order/labels, Switch Access, color contrast, reduced motion, haptic/audio feedback, and landscape/tablet behavior. |
| CL-BT-20 | Product outcome metrics, baselines/success criteria, targets, test cohort, and timeframe; analytics must remain content-free. |
| CL-BT-22 | Is camera off by default on every call, who may enable it, is remote video rendered when only one side enables video, and what happens on permission denial, camera-in-use, foreground/background transitions, or device rotation? |
| CL-BT-23 | What exact Morse reference content and ordering are approved: `A-Z`/`0-9` only or also punctuation/quick phrases; full sheet, searchable sheet, or collapsible panel; and must it remain visible simultaneously with the draft or only remain one accessible action away? |
| CL-BT-24 | What does “Send prominently visible at all times during text drafting” mean across IME open/closed, landscape, split screen, TalkBack focus, Switch Access scanning, and maximum supported font/display scale? |

## d) Build Plan

Code generation remains gated until M0 is reviewed and approved.

### M0 — Decisions, contracts, and production skeleton

Scope: close open P0 gates CL-BT-02..10 and CL-BT-21, align the six SPEC files and prototype target with resolved CL-BT-01/RD-BT-01..07, define OpenAPI/realtime/signaling schemas, choose backend/regions/TURN and the TTS-to-WebRTC media topology, document retention and threat model, and repair/pin the Gradle wrapper and CI design for reproducible builds.

Definition of Done:

- All P0 decisions have an owner, approved answer, and traceability to BR/UC/US/API/state contracts.
- No contradiction remains between business rules, user flow, wireframe, use cases, user stories, and the official one-button press-hold/release Morse UI.
- `MessageDraft` provenance represents all approved input paths, while one universal Send contract remains the only transition that can request TTS.
- The send contract distinguishes backend acceptance from WebRTC playback acknowledgement and defines server-side idempotency, timeout, cancellation, reconnect, and retry behavior.
- Root `build.gradle.kts` and `settings.gradle.kts` remain the canonical Android project entry; Gradle wrapper commands work from this directory in a clean environment.
- Security/data-flow threat model covers token theft, IDOR, signaling injection, replay, abuse, secret handling, and content deletion.
- Release builds cannot select demo/fake adapters; environments and secret ownership are documented.
- Product approves measurable MVP success criteria and accessibility test participants.

### M1 — App foundation, identity, profile, and social graph

Scope: modular Android architecture, localization, Firebase Auth, Bridge ID profile, friend requests/relationships, backend token verification, Firestore schema/rules, and real first-run/contact experience.

Definition of Done:

- A new user can authenticate, create a valid unique Bridge ID, sign out/in, and recover the correct profile in Vietnamese and English.
- Cold start is auth-aware: signed-out users see a functional login screen and signed-in users see the real contact/home surface; no generic landing page blocks either path.
- Two real test accounts can send, accept, decline/cancel as approved, and list friend relationships across process restarts.
- All mutations infer the operator from the verified token; IDOR and Firestore Rules tests pass.
- Loading, empty, offline, retry, expired-auth, and accessibility states are implemented.
- No communication content or secret appears in logs, analytics, resources, APK inspection, or Firestore.

### M2 — Incoming call, signaling, and real WebRTC audio/video

Scope: call creation/accept/end, FCM, Firestore signaling, STUN/TURN, `PeerConnection`, audio routing, optional camera/video tracks, lifecycle, timeout, and reconnect.

Definition of Done:

- Two authenticated devices establish bidirectional audio carried only by WebRTC on Wi-Fi, cellular, and a forced TURN route.
- Each user can turn their own camera on/off without interrupting audio; enabled video reaches only the remote peer through WebRTC, and camera permission/failure states remain honest.
- Incoming call works in the approved foreground/background/process states and respects notification permission behavior.
- Server-authoritative state/end reasons remain correct under duplicate, late, concurrent, unauthorized, and race requests.
- Media substate accurately reports negotiation, available, reconnecting, failed, and closed; failures never present a fake active call.
- Firebase/FCM payloads contain no audio or frame-by-frame caption stream; signaling documents expire automatically.

### M3 — Streaming captions and final translation

Scope: approved A-audio ingestion path, Speech-to-Text streaming, ordered interim/final captions, final-only Translation, reconnect/stream rotation, simultaneous source/translation display.

Definition of Done:

- B hears original WebRTC audio and simultaneously sees correctly labeled interim/final source captions.
- Final captions optionally show English/Vietnamese translation without replacing the source.
- Duplicate/out-of-order events, provider timeout, stream rotation, network handover, and partial provider failure are tested.
- Measured caption latency and quality meet the approved M0 thresholds on the agreed Vietnamese/English dataset.
- Raw audio and unsaved text are absent from durable stores and content-bearing logs.

### M4 — Morse draft, Gemini suggestions, confirmed TTS over WebRTC

Scope: approved Morse interaction, accessible Morse alphabetical reference, keyboard/manual input, decoder/settings, one shared editable `MessageDraft`, backend Gemini adapter, explicit suggestion selection, persistent universal Send/idempotency, Cloud TTS, and WebRTC audio injection.

Definition of Done:

- The approved large-control Morse flow works with representative dot/dash, character, word, invalid, undo, and adjustable-timing cases.
- The approved `A-Z`/`0-9` Morse reference is reachable during the call without losing the current draft, is usable with TalkBack/Switch Access and large text, and does not obscure the persistent Send action.
- B can type directly or use Morse and then review/edit the same draft; zero to five suggestions are clearly AI-generated, never preselected, and never alter the draft until selected.
- One prominent `GỬI`/`SEND` button remains on-screen while drafting for every supported input path—including with the keyboard/IME and Morse reference open—is disabled for empty/invalid drafts, and submits exactly the final on-screen `MessageDraft`.
- Pressing Send shows pending/success/failure/retry states; success is never shown before backend acceptance, and retry cannot make A hear the message twice.
- No TTS request or playback occurs before B presses Send, regardless of the draft's input source.
- A hears the exact confirmed text through WebRTC once; the UI does not claim delivery until the approved playback acknowledgement, and timeout/retry/reconnect/provider errors do not duplicate or change speech.
- Gemini/TTS keys and service credentials are backend-only; release traffic reaches real production adapters, not mocks.

### M5 — Consent, privacy lifecycle, and observability

Scope: bilateral transcript decision, ephemeral TTL, save/purge job, transcript access/deletion if approved, consent evidence, safe metrics, abuse/rate limits, and operational dashboards.

Definition of Done:

- Both `Granted` decisions are required atomically; either `Declined`, missing, or expired decision purges all transient communication content.
- Saved transcript contents, encryption, access controls, TTL, deletion/export, and revocation behavior match the approved policy.
- Automated retention tests prove raw audio, translations, Morse/raw timing, drafts, suggestions, and generated audio are not retained by default.
- Logs/traces/metrics contain IDs and operational metadata only where approved, never communication content or secrets.
- Alerting covers auth failures, call setup failures, TURN use/failure, provider latency/error, and purge-job failure.

### M6 — Accessibility validation, hardening, and release candidate

Scope: full regression, real-device/network matrix, accessibility study, security review, performance/cost/load tests, privacy/Play declarations, and release pipeline.

Definition of Done:

- All BR-BT, UC-BT, and US-BT acceptance tests are traceable and passing in CI/staging.
- Representative users complete the core call and Morse-to-voice tasks against approved success criteria; findings and remaining limitations are documented.
- TalkBack, Switch Access, large font/display size, contrast, touch targets, audio routing, and interruption scenarios pass on the approved device matrix.
- Backend/API/Firestore/FCM/WebRTC security tests and an independent release review have no unresolved critical/high findings.
- Signed release artifact is reproducible, contains no test adapters/secrets, and passes the approved Play pre-launch/privacy checklist.

## e) Testing Strategy

### Test pyramid and frameworks

| Layer | Proposed tools | Coverage |
|---|---|---|
| Kotlin unit | JUnit 4 or approved JUnit 5 setup, `kotlinx-coroutines-test`, Turbine, property-based tests where useful | Reducers, validators, Morse timing/decoder/reference mapping, camera/media state transitions, send/playback idempotency, localization formatting. |
| Android UI | Compose UI Test, AndroidX Test, Espresso interoperability, Robolectric only where it adds value | Screen states, persistent Send with IME/reference/large text, explicit AI selection/send, camera toggle, navigation, process recreation, permission/error states, semantics. |
| Accessibility | Compose semantics assertions, Android Accessibility Test Framework/Accessibility Scanner, manual TalkBack and Switch Access | Labels/order, Morse reference navigation, persistent Send reachability, focus, touch targets, contrast, dynamic type, live regions, non-color cues, motor-access behavior. |
| Backend unit/integration | Framework selected with backend runtime; Firebase Auth/Firestore Emulator Suite; provider contract fixtures; property/concurrency tests | Token verification, operator inference, authorization, state races, Rules, TTL/purge, rate limits, adapter mapping. |
| API contract | OpenAPI validation and generated-client compatibility checks | Request/response/error compatibility, auth requirements, idempotency, internal-only endpoints. |
| WebRTC integration | Android instrumentation on two devices/emulators plus a controllable TURN server/network environment | Offer/answer/ICE, forced relay, reconnect, handover, audio/video tracks, camera toggle, TTS injection and playback acknowledgement, teardown and leak checks. |
| Provider staging | Dedicated non-production Google Cloud/Firebase projects using real adapters and quotas | STT streaming, final Translation, TTS audio, Gemini 0..5 outputs, timeout/error mapping. Mocks/fakes are limited to automated isolation tests. |
| Security/privacy | Firestore Rules tests, API negative tests, dependency/secret scanning, APK inspection, log/data-store inspection, threat-model abuse cases | IDOR, spoofed identity/end reason, replay, signaling injection, unauthorized transcript access, secret leakage, retention violations. |
| Performance/reliability | Macrobenchmark where applicable, Android profiler, backend load tests, network shaping, long-call/soak tests | Startup, call setup, caption latency, memory/battery, stream rotation, TURN load, provider quota/backoff, cost envelope. |

### Required deterministic test data

- Two or more Firebase test users with fixed Bridge IDs, accepted/non-accepted/blocked relationships, expired tokens, and unauthorized cross-session access attempts.
- Synthetic/non-sensitive Vietnamese and English speech clips covering different speakers, pace, noise, silence, code-switching, numbers, names, and provider-unavailable cases. No production user recordings without approved consent.
- Expected final captions and translations reviewed by bilingual humans; interim text is evaluated for behavior and latency, not exact string equality.
- Complete International Morse vectors for approved `A-Z`, `0-9`, separators, boundary press durations, invalid prefixes/sequences, undo, and long-pause cases.
- Golden UI/reference data proving every displayed `A-Z`/`0-9` mapping matches the decoder and remains navigable at each approved locale, font scale, orientation, and accessibility mode.
- Drafts created by keyboard, Morse, selected suggestion, quick phrase, and mixed edits; include empty/min/max length, Unicode/diacritics, punctuation, unsafe/ambiguous language, and repeated Send/retry requests with stable idempotency keys.
- Gemini fixtures containing 0, 1, 5, and more-than-5 candidate outputs to prove server truncation/validation and no auto-selection.
- Consent matrix: `Granted/Granted`, `Granted/Declined`, `Declined/Granted`, missing response, late response, duplicate decision, changed decision before finalization, and expiry/purge failure.
- Network matrix: same Wi-Fi, different Wi-Fi, cellular, IPv6 where available, forced TURN, high latency/jitter/loss, offline/online transition, and app/process interruption.
- Camera matrix: permission granted/denied/revoked, no front camera, camera already in use, local on/off combinations, remote video absent, background/foreground, rotation, and audio continuity while video changes.

### Regression plan for the reported bugs

#### BUG-BT-01 — Send action hidden

- Compose UI tests enter a draft through keyboard, decoded Morse, selected Gemini fixture, and mixed edits, then assert exactly one enabled Send action is displayed and has a visible bounds intersection with the viewport.
- Repeat with the IME open, Morse reference open, zero/five suggestions, maximum draft length, Vietnamese/English, portrait/landscape, approved font/display scales, and TalkBack/Switch Access traversal.
- Scroll captions, suggestions, and reference content to their extremes; the persistent Send action must remain on-screen, must not overlap the draft or system bars, and must submit the exact visible draft.

#### BUG-BT-02 — Send disconnected from backend WebRTC/TTS

- A release/staging build must authenticate two real Firebase test users, create a real `CallSession`, establish WebRTC audio (including forced TURN), and use the real backend Cloud TTS adapter. No preview/mock sender may be selectable.
- On B, press Send once and correlate a content-free idempotency key across client request, authorized backend command, one TTS synthesis, one WebRTC injection, and A-side playback acknowledgement. Assert A hears the exact draft once.
- Test expired/invalid token, non-participant, call not connected, WebRTC reconnecting, TTS timeout/error, backend timeout after acceptance, duplicated tap, process recreation, and retry. The draft is retained on failure; the same idempotency key is reused for an unchanged retry; no path speaks twice or reports “sent/heard” early.
- Inspect Firestore, backend stores, logs, analytics, device storage, and provider traces after success/failure to prove raw audio, captions, translations, Morse, `MessageDraft`, and `VoiceOutput` were not durably saved by this flow.

### Critical end-to-end acceptance journeys

1. Authenticate two users, create profiles, add by Bridge ID, and accept friendship.
2. A calls B; B receives the approved FCM UX and accepts; bidirectional WebRTC audio becomes available. Toggle each camera independently and verify optional standard video uses the same WebRTC peer connection without interrupting audio.
3. A speaks Vietnamese/English; B hears original audio while seeing interim/final source captions and optional final translation.
4. Open and navigate the Morse `A-Z`/`0-9` reference, then verify two explicit input paths: B types a draft; B separately presses, holds, and releases the single large Morse button to create a draft. In each path B can edit, optionally select one of at most five labeled AI suggestions, and must press the same Send button, which remains visible throughout drafting.
5. Confirm no input path requests TTS before Send; after Send, A hears the exact final on-screen message once through WebRTC.
6. Inject STT, Translation, Gemini, TTS, TURN, signaling, permission, and network failures independently; unaffected capabilities remain usable and state remains honest.
7. End the call from each valid state and validate server-derived `endReason`, teardown, TTL, and no content retention.
8. Exercise every consent combination and prove only bilateral `Granted` produces an authorized transcript; all other paths purge transient content.

### Release gates

- **Functional:** 100% of Must-scope BR/UC/US acceptance tests pass; no open critical/high defect.
- **Accessibility:** approved automated checks plus manual target-user tasks pass; results are separated from general accessibility guidelines.
- **Security/privacy:** no client-side secret, no unauthorized mutation/read, and automated retention evidence passes.
- **Media:** real WebRTC audio and enabled video pass direct and forced-TURN paths; camera off does not break audio; no Firebase audio/video path exists; no video-analysis component exists.
- **User control:** zero auto-send/auto-speak cases for keyboard, Morse, suggestions, quick phrases, or any other approved input across unit, UI, integration, and failure/retry tests.
- **Operations:** dashboards, alerts, quota/backoff, deletion jobs, rollback, and incident ownership are exercised in staging.

## Approval boundary

Approval of this review authorizes M0 contract work only unless the user explicitly confirms code generation. Production coding should begin only after the P0 decisions are recorded and the resulting SPEC changes are reviewed.

## Sources reviewed

- Official BridgeTalk requirements supplied for this review on 2026-08-27 (highest authority for scope and constraints)
- `spec/business-rules.md`
- `spec/dataflow.md`
- `spec/userflow.md`
- `spec/wireframe.md`
- `spec/usecase.md`
- `spec/userstory.md`
- `research/google-production-stack.md`
- `app/src/main/java/com/bridgetalk/app/MainActivity.kt`
- root `build.gradle.kts`, root `settings.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml`, `README.md`, and `BRIDGETALK-SUBMISSION.md`
