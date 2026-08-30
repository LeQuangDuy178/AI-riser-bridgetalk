# BridgeTalk backend

Real HTTPS service that the Android `ConfirmDraftMessageSender` calls when the
caller presses **Send**. The backend is the only place that holds secrets
(Gemini API key, Google service account, Firestore project) and the only place
that runs Google Cloud Text-to-Speech. The Android app never sees a secret.

## Endpoints

| Method | Path                              | Purpose                                                    |
| ------ | --------------------------------- | ---------------------------------------------------------- |
| GET    | `/health`                         | Liveness + DEMO_MODE flag.                                 |
| POST   | `/api/suggestions`                | Up to 5 Gemini reply suggestions (backend-only AI).        |
| POST   | `/api/calls/:callId/send`         | Confirm a draft, run Cloud TTS, persist consent-gated transcript. |
| GET    | `/api/calls/:callId/transcript`   | Read previously-stored transcript segments (DEMO_MODE only). |

## Run locally (no cloud credentials)

```bash
cd backend
cp .env.example .env.local
# leave DEMO_MODE=true
npm install --omit=optional
npm start
```

The backend listens on `http://localhost:8787` and the Android emulator reaches
it at `http://10.0.2.2:8787` (already configured in
`network_security_config.xml`).

## Production wiring

Set the following in the deploy environment (NEVER in source code):

* `FIREBASE_SERVICE_ACCOUNT` — path to a service account JSON with
  `Firebase Authentication Admin` permissions, OR set
  `GOOGLE_APPLICATION_CREDENTIALS` to a service account that has both
  Firebase Auth Admin and Cloud Text-to-Speech User.
* `FIRESTORE_PROJECT_ID` — Firestore project for consent-gated transcripts.
* `GEMINI_API_KEY` — Generative Language API key.
* `DEMO_MODE=false` — disables the demo bypass and the in-memory TTS tone.

The backend rejects `/api/calls/:callId/send` with 401 if no bearer token is
provided and `DEMO_MODE=false`. The operator (uid) is always inferred from the
verified Firebase ID token; `callerId` / `participantId` / `endReason` sent by
the client are ignored.
