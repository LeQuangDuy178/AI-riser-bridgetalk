'use strict';

const express = require('express');
const { authenticate } = require('./adapters/auth');
const { generateSuggestions, MAX_SUGGESTIONS } = require('./adapters/gemini');
const { synthesize } = require('./adapters/tts');
const { appendSegment, listDemoSegments } = require('./adapters/transcriptStore');

const MAX_MESSAGE_LENGTH = 500;
const MAX_IDEMPOTENCY_LENGTH = 80;
const IDEMPOTENCY_PATTERN = /^[A-Za-z0-9._:-]{1,80}$/;

function trim(value, max) {
  if (typeof value !== 'string') return '';
  return value.trim().slice(0, max);
}

function validateSendBody(body) {
  if (!body || typeof body !== 'object') {
    return { ok: false, reason: 'INVALID_BODY' };
  }
  const message = trim(body.message, MAX_MESSAGE_LENGTH);
  if (!message) {
    return { ok: false, reason: 'EMPTY_MESSAGE' };
  }
  const idempotencyKey = trim(body.idempotencyKey, MAX_IDEMPOTENCY_LENGTH);
  if (!idempotencyKey || !IDEMPOTENCY_PATTERN.test(idempotencyKey)) {
    return { ok: false, reason: 'INVALID_IDEMPOTENCY_KEY' };
  }
  const languageCode = typeof body.languageCode === 'string' && body.languageCode
    ? trim(body.languageCode, 16)
    : 'vi-VN';
  const recipientUid = typeof body.recipientUid === 'string'
    ? trim(body.recipientUid, 128)
    : '';
  const senderConsent = body.senderConsent === true;
  const recipientConsent = body.recipientConsent === true;
  return {
    ok: true,
    payload: { message, idempotencyKey, languageCode, recipientUid, senderConsent, recipientConsent },
  };
}

function createRouter() {
  const router = express.Router();

  router.get('/health', (_req, res) => {
    res.json({
      ok: true,
      service: 'bridgetalk-backend',
      demoMode: process.env.DEMO_MODE === 'true',
      uptimeSeconds: Math.floor(process.uptime()),
    });
  });

  router.post('/api/suggestions', express.json({ limit: '8kb' }), async (req, res) => {
    const draft = typeof req.body?.draft === 'string' ? req.body.draft : '';
    const languageCode = typeof req.body?.languageCode === 'string' && req.body.languageCode
      ? req.body.languageCode
      : 'vi-VN';
    const result = await generateSuggestions({ draft, languageCode });
    if (!result.ok) {
      return res.status(503).json({ error: result.reason, detail: result.detail });
    }
    return res.json({
      suggestions: result.suggestions.slice(0, MAX_SUGGESTIONS),
      source: result.source,
    });
  });

  router.post('/api/calls/:callId/send', express.json({ limit: '8kb' }), async (req, res) => {
    const auth = await authenticate(req);
    if (!auth.ok) {
      return res.status(401).json({ error: auth.reason });
    }
    const validation = validateSendBody(req.body);
    if (!validation.ok) {
      return res.status(400).json({ error: validation.reason });
    }
    const { callId } = req.params;
    if (!callId || callId.length > 128) {
      return res.status(400).json({ error: 'INVALID_CALL_ID' });
    }
    const { message, idempotencyKey, languageCode, recipientUid, senderConsent, recipientConsent } = validation.payload;

    const tts = await synthesize(message, languageCode);
    if (!tts.ok) {
      return res.status(503).json({ error: tts.reason });
    }

    const transcript = await appendSegment({
      callId,
      senderUid: auth.uid,
      recipientUid: recipientUid || 'unknown',
      text: message,
      senderConsent,
      recipientConsent,
    });

    res.set('Cache-Control', 'no-store');
    return res.json({
      status: 'accepted',
      callId,
      operator: { uid: auth.uid },
      message,
      idempotencyKey,
      voiceOutput: {
        source: tts.source,
        encoding: tts.encoding,
        byteLength: tts.audio.length,
        contentType: tts.source === 'cloud' ? 'audio/ogg' : 'audio/wav',
      },
      transcript: transcript.stored ? { stored: true, sink: transcript.source } : { stored: false, reason: transcript.reason },
    });
  });

  router.get('/api/calls/:callId/transcript', async (req, res) => {
    const auth = await authenticate(req);
    if (!auth.ok) {
      return res.status(401).json({ error: auth.reason });
    }
    res.json({ segments: listDemoSegments(req.params.callId) });
  });

  return router;
}

module.exports = { createRouter, validateSendBody, MAX_MESSAGE_LENGTH };
