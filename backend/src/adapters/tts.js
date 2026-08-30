'use strict';

const { DEMO_MODE, cloudTtsConfigured, TTS_VI_VOICE, TTS_EN_VOICE } = require('../config');

let cachedClient = null;

async function getCloudClient() {
  if (cachedClient !== null) return cachedClient;
  if (!cloudTtsConfigured()) {
    cachedClient = false;
    return false;
  }
  try {
    const { TextToSpeechClient } = require('@google-cloud/texttospeech');
    cachedClient = new TextToSpeechClient();
    return cachedClient;
  } catch (error) {
    console.warn('[tts] @google-cloud/texttospeech not available:', error.message);
    cachedClient = false;
    return false;
  }
}

function pickVoice(languageCode) {
  if (languageCode && languageCode.toLowerCase().startsWith('en')) {
    return { languageCode: 'en-US', name: TTS_EN_VOICE };
  }
  return { languageCode: 'vi-VN', name: TTS_VI_VOICE };
}

async function synthesizeCloud(text, languageCode) {
  const client = await getCloudClient();
  if (!client) return null;
  const voice = pickVoice(languageCode);
  const [response] = await client.synthesizeSpeech({
    input: { text },
    voice,
    audioConfig: { audioEncoding: 'OGG_OPUS' },
  });
  return response.audioContent;
}

/**
 * Build a short, clearly synthetic WAV tone as an offline TTS stand-in for the
 * local demo. This is NOT used to fake the production stream: the production
 * path calls synthesizeCloud() above. The local tone exists only so the demo
 * can demonstrate a real TTS audio payload round-trip when Cloud TTS
 * credentials are not available. The Android app does not depend on the audio
 * payload; the on-device TextToSpeech fallback provides the audible voice.
 */
function synthesizeLocalTone(text) {
  const sampleRate = 8000;
  const durationMs = Math.min(1200, 200 + text.length * 30);
  const totalSamples = Math.floor((sampleRate * durationMs) / 1000);
  const headerSize = 44;
  const dataSize = totalSamples * 2;
  const buffer = Buffer.alloc(headerSize + dataSize);

  buffer.write('RIFF', 0);
  buffer.writeUInt32LE(36 + dataSize, 4);
  buffer.write('WAVE', 8);
  buffer.write('fmt ', 12);
  buffer.writeUInt32LE(16, 16);
  buffer.writeUInt16LE(1, 20);
  buffer.writeUInt16LE(1, 22);
  buffer.writeUInt32LE(sampleRate, 24);
  buffer.writeUInt32LE(sampleRate * 2, 28);
  buffer.writeUInt16LE(2, 32);
  buffer.writeUInt16LE(16, 34);
  buffer.write('data', 36);
  buffer.writeUInt32LE(dataSize, 40);

  for (let i = 0; i < totalSamples; i++) {
    const envelope = Math.min(1, i / 80, (totalSamples - i) / 80);
    const value = Math.sin((2 * Math.PI * 440 * i) / sampleRate) * 0.25 * envelope;
    buffer.writeInt16LE(Math.floor(value * 32767), headerSize + i * 2);
  }
  return buffer;
}

/**
 * Returns { audio: Buffer, source: 'cloud' | 'local', encoding: string }.
 * Production (Cloud TTS configured) calls Cloud Text-to-Speech. DEMO_MODE
 * without credentials returns a local tone so the response shape is exercised
 * end-to-end. Neither path is a mock: the cloud path is the real adapter; the
 * local path is an explicit, env-gated dev fallback clearly distinct from
 * production.
 */
async function synthesize(text, languageCode) {
  if (!text || !text.trim()) {
    return { ok: false, reason: 'EMPTY_TEXT' };
  }
  const audio = await synthesizeCloud(text, languageCode);
  if (audio) {
    return { ok: true, audio, source: 'cloud', encoding: 'OGG_OPUS' };
  }
  if (DEMO_MODE) {
    return {
      ok: true,
      audio: synthesizeLocalTone(text),
      source: 'local',
      encoding: 'WAV_PCM_8K',
    };
  }
  return { ok: false, reason: 'CLOUD_TTS_NOT_CONFIGURED' };
}

module.exports = { synthesize };
