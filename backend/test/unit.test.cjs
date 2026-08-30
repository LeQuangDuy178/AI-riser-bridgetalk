'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

process.env.DEMO_MODE = 'true';
process.env.BRIDGETALK_NO_ENVFILE = '1';
delete process.env.GEMINI_API_KEY;
process.env.GEMINI_API_KEY = '';
delete process.env.FIREBASE_SERVICE_ACCOUNT;
delete process.env.GOOGLE_APPLICATION_CREDENTIALS;
delete process.env.FIRESTORE_PROJECT_ID;

const { validateSendBody, MAX_MESSAGE_LENGTH } = require('../src/routes');
const { generateSuggestions, MAX_SUGGESTIONS } = require('../src/adapters/gemini');
const { synthesize } = require('../src/adapters/tts');
const { authenticate } = require('../src/adapters/auth');

test('validateSendBody rejects empty message and bad idempotency keys', () => {
  assert.equal(validateSendBody({}).ok, false);
  assert.equal(validateSendBody({ message: '   ', idempotencyKey: 'k1' }).ok, false);
  assert.equal(validateSendBody({ message: 'hi', idempotencyKey: 'has space' }).ok, false);
});

test('validateSendBody trims and accepts a normal payload', () => {
  const r = validateSendBody({ message: '  xin chào  ', idempotencyKey: 'k-1', languageCode: 'vi-VN' });
  assert.equal(r.ok, true);
  assert.equal(r.payload.message, 'xin chào');
  assert.equal(r.payload.idempotencyKey, 'k-1');
  assert.equal(r.payload.languageCode, 'vi-VN');
});

test('validateSendBody caps message length', () => {
  const long = 'a'.repeat(MAX_MESSAGE_LENGTH + 10);
  const r = validateSendBody({ message: long, idempotencyKey: 'k1' });
  assert.equal(r.ok, true);
  assert.equal(r.payload.message.length, MAX_MESSAGE_LENGTH);
});

test('generateSuggestions returns a deterministic fallback when no API key', async () => {
  const r = await generateSuggestions({ draft: '', languageCode: 'vi-VN' });
  assert.equal(r.ok, true);
  assert.equal(r.source, 'fallback');
  assert.ok(r.suggestions.length > 0);
  assert.ok(r.suggestions.length <= MAX_SUGGESTIONS);
  assert.ok(r.suggestions.every((s) => typeof s === 'string' && s.length > 0));
});

test('synthesize returns a local tone in DEMO_MODE without cloud creds', async () => {
  const r = await synthesize('xin chào', 'vi-VN');
  assert.equal(r.ok, true);
  assert.equal(r.source, 'local');
  assert.ok(r.audio.length > 0);
  assert.equal(r.encoding, 'WAV_PCM_8K');
});

test('synthesize rejects empty text', async () => {
  const r = await synthesize('   ', 'vi-VN');
  assert.equal(r.ok, false);
  assert.equal(r.reason, 'EMPTY_TEXT');
});

test('authenticate accepts demo caller in DEMO_MODE without a token', async () => {
  const req = { headers: {} };
  const r = await authenticate(req);
  assert.equal(r.ok, true);
  assert.equal(r.uid, 'demo-user');
});

test('authenticate rejects bearer token when firebase-admin is not configured', async () => {
  const req = { headers: { authorization: 'Bearer some.jwt.token' } };
  const r = await authenticate(req);
  assert.equal(r.ok, false);
  assert.equal(r.reason, 'FIREBASE_AUTH_NOT_CONFIGURED');
});
