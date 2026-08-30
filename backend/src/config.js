'use strict';

const fs = require('node:fs');
const path = require('node:path');

function loadEnv(file) {
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*([^#=]+)=(.*)$/);
    if (match && !process.env[match[1].trim()]) {
      let value = match[2].trim();
      if ((value.startsWith('"') && value.endsWith('"')) ||
          (value.startsWith("'") && value.endsWith("'"))) {
        value = value.slice(1, -1);
      }
      process.env[match[1].trim()] = value;
    }
  }
}

if (process.env.BRIDGETALK_NO_ENVFILE !== '1') {
  loadEnv(path.join(__dirname, '..', '.env.local'));
  loadEnv(path.join(__dirname, '..', '..', '.env.local'));
}

const PORT = Number(process.env.PORT || 8787);
const DEMO_MODE = process.env.DEMO_MODE === 'true';
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';

function firebaseConfigured() {
  return Boolean(
    process.env.FIREBASE_SERVICE_ACCOUNT || process.env.GOOGLE_APPLICATION_CREDENTIALS
  );
}

function cloudTtsConfigured() {
  return Boolean(process.env.GOOGLE_APPLICATION_CREDENTIALS);
}

function firestoreConfigured() {
  return Boolean(process.env.GOOGLE_APPLICATION_CREDENTIALS && process.env.FIRESTORE_PROJECT_ID);
}

module.exports = {
  PORT,
  DEMO_MODE,
  GEMINI_API_KEY,
  TTS_VI_VOICE: process.env.TTS_VI_VOICE || 'vi-VN-Neural2-A',
  TTS_EN_VOICE: process.env.TTS_EN_VOICE || 'en-US-Neural2-A',
  firebaseConfigured,
  cloudTtsConfigured,
  firestoreConfigured,
};
