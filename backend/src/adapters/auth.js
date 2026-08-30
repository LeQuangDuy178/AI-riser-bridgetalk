'use strict';

const { DEMO_MODE, firebaseConfigured } = require('../config');

let cachedApp = null;
let cachedVerifier = null;

async function getVerifier() {
  if (cachedVerifier !== null) return cachedVerifier;

  if (!firebaseConfigured()) {
    cachedVerifier = false;
    return false;
  }

  try {
    // Lazy require so the dependency is optional; production deployments
    // install firebase-admin and provide credentials. The Android app and the
    // local demo never need to install it.
    const admin = require('firebase-admin');
    if (!cachedApp) {
      if (process.env.FIREBASE_SERVICE_ACCOUNT) {
        const serviceAccount = require(
          require('node:path').resolve(process.env.FIREBASE_SERVICE_ACCOUNT)
        );
        cachedApp = admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
      } else {
        cachedApp = admin.initializeApp();
      }
    }
    cachedVerifier = admin.auth();
    return cachedVerifier;
  } catch (error) {
    console.warn('[auth] firebase-admin not available:', error.message);
    cachedVerifier = false;
    return false;
  }
}

/**
 * Verifies the operator from the Authorization: Bearer <token> header.
 * The backend NEVER trusts callerId/participantId/endReason from the client;
 * the verified token is the only source of truth for who is sending.
 *
 * Returns { ok: true, uid } on success.
 * Returns { ok: false, reason } on failure. In DEMO_MODE, unauthenticated
 * requests are accepted and attributed to the synthetic uid "demo-user" so the
 * local walk-through can run without a Firebase project. NEVER enable
 * DEMO_MODE in production.
 */
async function authenticate(req) {
  const header = req.headers['authorization'] || '';
  const match = /^Bearer\s+(.+)$/i.exec(header);
  if (match) {
    const verifier = await getVerifier();
    if (!verifier) {
      return { ok: false, reason: 'FIREBASE_AUTH_NOT_CONFIGURED' };
    }
    try {
      const decoded = await verifier.verifyIdToken(match[1]);
      return { ok: true, uid: decoded.uid };
    } catch (error) {
      return { ok: false, reason: 'INVALID_TOKEN' };
    }
  }

  if (DEMO_MODE) {
    return { ok: true, uid: 'demo-user' };
  }
  return { ok: false, reason: 'MISSING_BEARER_TOKEN' };
}

module.exports = { authenticate };
