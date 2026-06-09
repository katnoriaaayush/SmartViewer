import { GoogleGenAI } from '@google/genai';
import { readFileSync } from 'fs';

const MODEL = process.env.VERTEX_MODEL || 'gemini-2.0-flash-001';

// Lazy — instantiated only on first AI call so the server starts without credentials
let _ai = null;
function ai() {
  if (!_ai) {
    _ai = new GoogleGenAI({
      vertexai: true,
      project:  process.env.GOOGLE_CLOUD_PROJECT,
      location: process.env.GOOGLE_CLOUD_LOCATION || 'us-central1',
    });
  }
  return _ai;
}

function filePart(filePath, mimeType) {
  return {
    inlineData: {
      mimeType,
      data: readFileSync(filePath).toString('base64'),
    },
  };
}

function text(content) {
  return { text: content };
}

function parseJson(raw) {
  const start = raw.indexOf('{');
  const end   = raw.lastIndexOf('}');
  if (start === -1 || end === -1) throw new Error('No JSON object in model response');
  return JSON.parse(raw.slice(start, end + 1));
}

// Returns AsyncIterable<GenerateContentResponse>
export async function chatStream(filePath, mimeType, history, userMessage) {
  const chat = ai().chats.create({
    model:   MODEL,
    history: history.map(m => ({ role: m.role, parts: [text(m.content)] })),
  });
  return chat.sendMessageStream([filePart(filePath, mimeType), text(userMessage)]);
}

export async function summarize(filePath, mimeType, mode, opts = {}) {
  const basePrompts = {
    quick:    'Provide a concise summary of this document in 150–200 words covering the main points.',
    detailed: 'Provide a comprehensive, detailed summary covering all major topics, key arguments, and conclusions.',
    custom:   opts.customPrompt || 'Summarize this document.',
  };

  let prompt = basePrompts[mode];
  if (opts.targetAudience) prompt += ` Target audience: ${opts.targetAudience}.`;
  if (opts.wordLimit)      prompt += ` Keep it under ${opts.wordLimit} words.`;

  const result = await ai().models.generateContent({
    model:    MODEL,
    config:   { maxOutputTokens: mode === 'detailed' ? 4096 : 2048 },
    contents: [{ role: 'user', parts: [filePart(filePath, mimeType), text(prompt)] }],
  });
  return result.text;
}

export async function generateFlashcards(filePath, mimeType, count = 10) {
  const prompt = `Generate exactly ${count} flashcards from this document.
Return ONLY valid JSON, no markdown fences, no extra text:
{"flashcards":[{"front":"question","back":"answer","difficulty":"easy|medium|hard","page":1}]}`;

  const result = await ai().models.generateContent({
    model:    MODEL,
    config:   { maxOutputTokens: 4096, temperature: 0.2 },
    contents: [{ role: 'user', parts: [filePart(filePath, mimeType), text(prompt)] }],
  });
  return parseJson(result.text).flashcards;
}

export async function generateInsights(filePath, mimeType) {
  const prompt = `Analyse this document and return ONLY valid JSON, no markdown:
{
  "keyPoints":   ["point 1", "point 2"],
  "entities":    [{"name": "Name", "type": "PERSON|ORGANIZATION|DATE|LOCATION|OTHER"}],
  "actionItems": ["item 1"],
  "topics":      ["topic 1"]
}`;

  const result = await ai().models.generateContent({
    model:    MODEL,
    config:   { maxOutputTokens: 4096, temperature: 0.2 },
    contents: [{ role: 'user', parts: [filePart(filePath, mimeType), text(prompt)] }],
  });
  return parseJson(result.text);
}

export async function generateIndex(filePath, mimeType) {
  const prompt = `Extract this document's table of contents / structure. Return ONLY valid JSON:
{
  "sections": [{"title": "Section Title", "page": 1, "summary": "one sentence", "level": 1}]
}`;

  const result = await ai().models.generateContent({
    model:    MODEL,
    config:   { maxOutputTokens: 2048, temperature: 0.1 },
    contents: [{ role: 'user', parts: [filePart(filePath, mimeType), text(prompt)] }],
  });
  return parseJson(result.text).sections;
}

export async function explain(filePath, mimeType, selectedText, mode) {
  const instructions = {
    explain:  `Explain the following text from the document clearly and thoroughly:\n"${selectedText}"`,
    simplify: `Rewrite the following text in simple, easy-to-understand language:\n"${selectedText}"`,
    define:   `Define all technical terms, acronyms, and domain-specific concepts in:\n"${selectedText}"`,
  };

  const result = await ai().models.generateContent({
    model:    MODEL,
    config:   { maxOutputTokens: 1024 },
    contents: [{ role: 'user', parts: [filePart(filePath, mimeType), text(instructions[mode])] }],
  });
  return result.text;
}
