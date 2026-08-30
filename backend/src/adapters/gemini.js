'use strict';

const { GEMINI_API_KEY, DEMO_MODE } = require('../config');

const DEFAULT_MODELS = ['gemini-3.6-flash', 'gemini-3.7-flash', 'gemini-2.5-flash'];
const MAX_SUGGESTIONS = 5;

const FALLBACK_BY_LANG = {
  'vi-VN': ['Có', 'Không', 'Chờ tôi', 'Tôi cần giúp đỡ', 'Cảm ơn bạn'],
  'en-US': ['Yes', 'No', 'Wait for me', 'I need help', 'Thank you'],
};

function fallbackSuggestions(languageCode) {
  return (FALLBACK_BY_LANG[languageCode] || FALLBACK_BY_LANG['en-US']).slice(0, MAX_SUGGESTIONS);
}

function systemPrompt(languageCode) {
  const lang = (languageCode || 'en-US').toLowerCase().startsWith('vi')
    ? 'Vietnamese'
    : 'English';
  return [
    'You generate short reply suggestions for a non-speaking call participant.',
    `Reply in ${lang}. Each suggestion must be at most 6 words, polite, and safe.`,
    'Return strict JSON of the form {"suggestions": ["...", "..."]}.',
    `Return at most ${MAX_SUGGESTIONS} suggestions, ordered most useful first.`,
    'Do not include greetings, names, or speaker labels. Do not translate the user text; produce ready-to-send replies that the caller would say next.',
  ].join(' ');
}

function userPrompt(draft) {
  return draft && draft.trim()
    ? `The caller is composing this draft: "${draft.trim()}". Produce up to ${MAX_SUGGESTIONS} ready-to-send replies.`
    : `The caller has not typed a draft yet. Produce up to ${MAX_SUGGESTIONS} common short replies a non-speaking caller would want.`;
}

async function callGemini(model, apiKey, languageCode, draft) {
  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-goog-api-key': apiKey },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: systemPrompt(languageCode) }] },
        contents: [{ parts: [{ text: userPrompt(draft) }] }],
        generationConfig: {
          responseMimeType: 'application/json',
          temperature: 0.4,
          maxOutputTokens: 256,
        },
      }),
    }
  );
  if (!response.ok) {
    throw new Error(`gemini ${model} returned ${response.status}`);
  }
  const payload = await response.json();
  const text = (payload.candidates || [])
    .flatMap((c) => c.content?.parts || [])
    .map((p) => p.text || '')
    .filter((t) => t.length > 0)
    .pop() || '';
  if (!text) throw new Error('gemini returned no text');
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch (error) {
    const match = text.match(/\{[\s\S]*\}/);
    if (!match) throw new Error('gemini returned non-JSON');
    parsed = JSON.parse(match[0]);
  }
  const list = Array.isArray(parsed.suggestions) ? parsed.suggestions : [];
  const cleaned = list
    .map((s) => (typeof s === 'string' ? s.trim() : ''))
    .filter((s) => s.length > 0 && s.length <= 80)
    .slice(0, MAX_SUGGESTIONS);
  if (cleaned.length === 0) throw new Error('gemini returned empty suggestions');
  return cleaned;
}

/**
 * Returns up to 5 Gemini-generated reply suggestions. The Android app must
 * NEVER call Gemini directly; the API key lives only on the backend. When
 * Gemini is not configured the endpoint still returns a deterministic
 * language-appropriate fallback so the demo never blocks the user.
 */
async function generateSuggestions({ draft, languageCode }) {
  const safeLang = (languageCode || 'en-US');
  if (!GEMINI_API_KEY) {
    return { ok: true, suggestions: fallbackSuggestions(safeLang), source: 'fallback' };
  }
  const preferred = process.env.GEMINI_MODEL || DEFAULT_MODELS[0];
  const models = Array.from(new Set([preferred, ...DEFAULT_MODELS]));
  let lastError;
  for (const model of models) {
    try {
      const suggestions = await callGemini(model, GEMINI_API_KEY, safeLang, draft);
      return { ok: true, suggestions, source: 'gemini' };
    } catch (error) {
      lastError = error;
      console.warn('[suggestions]', error.message);
    }
  }
  if (DEMO_MODE) {
    return { ok: true, suggestions: fallbackSuggestions(safeLang), source: 'fallback' };
  }
  return { ok: false, reason: 'GEMINI_UNAVAILABLE', detail: lastError?.message };
}

module.exports = { generateSuggestions, MAX_SUGGESTIONS };
