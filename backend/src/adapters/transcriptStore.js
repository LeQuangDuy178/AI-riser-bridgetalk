'use strict';

const { DEMO_MODE, firestoreConfigured } = require('../config');

const memoryTranscripts = new Map();
let cachedDb = null;

async function getDb() {
  if (cachedDb !== null) return cachedDb;
  if (!firestoreConfigured()) {
    cachedDb = false;
    return false;
  }
  try {
    const { Firestore } = require('@google-cloud/firestore');
    cachedDb = new Firestore({ projectId: process.env.FIRESTORE_PROJECT_ID });
    return cachedDb;
  } catch (error) {
    console.warn('[transcript] @google-cloud/firestore not available:', error.message);
    cachedDb = false;
    return false;
  }
}

/**
 * Persists a transcript segment ONLY when BOTH participants have granted
 * consent. Default behaviour is to not save anything. The data retention
 * rules forbid saving raw audio, captions, translations, Morse, MessageDraft,
 * or default VoiceOutput; this adapter only stores consented transcript text
 * keyed by callId and participant uids.
 */
async function appendSegment({ callId, senderUid, recipientUid, text, senderConsent, recipientConsent }) {
  if (!senderConsent || !recipientConsent) {
    return { stored: false, reason: 'CONSENT_NOT_GRANTED' };
  }
  if (!text || !text.trim()) {
    return { stored: false, reason: 'EMPTY_TEXT' };
  }
  const segment = {
    callId,
    senderUid,
    recipientUid,
    text: text.trim(),
    createdAt: new Date().toISOString(),
  };
  const db = await getDb();
  if (db) {
    await db.collection('transcripts').doc(callId).collection('segments').add(segment);
    return { stored: true, source: 'firestore' };
  }
  if (DEMO_MODE) {
    const list = memoryTranscripts.get(callId) || [];
    list.push(segment);
    memoryTranscripts.set(callId, list);
    return { stored: true, source: 'memory' };
  }
  return { stored: false, reason: 'FIRESTORE_NOT_CONFIGURED' };
}

function listDemoSegments(callId) {
  return memoryTranscripts.get(callId) || [];
}

module.exports = { appendSegment, listDemoSegments };
