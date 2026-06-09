import { Router }                                                             from 'express';
import { stmts, insertFlashcardsBatch }                                       from '../db.js';
import { summarize, generateFlashcards, generateInsights, generateIndex, explain } from '../gemini.js';

const router = Router();

// POST /api/documents/:id/summarize
router.post('/:id/summarize', async (req, res) => {
  const doc = stmts.getDocument.get(req.params.id);
  if (!doc) return res.status(404).json({ error: 'Document not found.' });

  const { mode = 'quick', customPrompt, targetAudience, wordLimit } = req.body;
  const validModes = ['quick', 'detailed', 'custom'];
  if (!validModes.includes(mode)) {
    return res.status(400).json({ error: `mode must be one of: ${validModes.join(', ')}` });
  }

  if (mode !== 'custom') {
    const cached = stmts.getSummary.get(doc.id, mode);
    if (cached) return res.json({ summary: cached.content, mode, cached: true });
  }

  try {
    const content = await summarize(doc.file_uri, doc.mime_type, mode, {
      customPrompt, targetAudience, wordLimit,
    });
    if (mode !== 'custom') stmts.upsertSummary.run({ documentId: doc.id, mode, content });
    res.json({ summary: content, mode, cached: false });
  } catch (err) {
    console.error('Summarize error:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/documents/:id/flashcards
router.post('/:id/flashcards', async (req, res) => {
  const doc = stmts.getDocument.get(req.params.id);
  if (!doc) return res.status(404).json({ error: 'Document not found.' });

  const cached = stmts.getFlashcards.all(doc.id);
  if (cached.length > 0) {
    return res.json({
      flashcards: cached.map(c => ({
        id: c.id, front: c.front, back: c.back, difficulty: c.difficulty, page: c.page,
      })),
      cached: true,
    });
  }

  const count = Math.min(Number(req.body.count) || 10, 20);
  try {
    const cards = await generateFlashcards(doc.file_uri, doc.mime_type, count);
    insertFlashcardsBatch(doc.id, cards);
    res.json({ flashcards: cards, cached: false });
  } catch (err) {
    console.error('Flashcards error:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/documents/:id/insights
router.get('/:id/insights', async (req, res) => {
  const doc = stmts.getDocument.get(req.params.id);
  if (!doc) return res.status(404).json({ error: 'Document not found.' });

  const cached = stmts.getInsights.get(doc.id);
  if (cached) {
    return res.json({
      keyPoints:   JSON.parse(cached.key_points),
      entities:    JSON.parse(cached.entities),
      actionItems: JSON.parse(cached.action_items),
      topics:      JSON.parse(cached.topics),
      cached: true,
    });
  }

  try {
    const data = await generateInsights(doc.file_uri, doc.mime_type);
    stmts.upsertInsights.run({
      documentId:  doc.id,
      keyPoints:   JSON.stringify(data.keyPoints),
      entities:    JSON.stringify(data.entities),
      actionItems: JSON.stringify(data.actionItems),
      topics:      JSON.stringify(data.topics),
    });
    res.json({ ...data, cached: false });
  } catch (err) {
    console.error('Insights error:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/documents/:id/index
router.get('/:id/index', async (req, res) => {
  const doc = stmts.getDocument.get(req.params.id);
  if (!doc) return res.status(404).json({ error: 'Document not found.' });

  const cached = stmts.getIndex.get(doc.id);
  if (cached) return res.json({ sections: JSON.parse(cached.sections), cached: true });

  try {
    const sections = await generateIndex(doc.file_uri, doc.mime_type);
    stmts.upsertIndex.run({ documentId: doc.id, sections: JSON.stringify(sections) });
    res.json({ sections, cached: false });
  } catch (err) {
    console.error('Index error:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/documents/:id/explain
router.post('/:id/explain', async (req, res) => {
  const doc = stmts.getDocument.get(req.params.id);
  if (!doc) return res.status(404).json({ error: 'Document not found.' });

  const { selectedText, mode = 'explain' } = req.body;
  if (!selectedText?.trim()) return res.status(400).json({ error: 'selectedText is required.' });

  const validModes = ['explain', 'simplify', 'define'];
  if (!validModes.includes(mode)) {
    return res.status(400).json({ error: `mode must be one of: ${validModes.join(', ')}` });
  }

  try {
    const explanation = await explain(doc.file_uri, doc.mime_type, selectedText, mode);
    res.json({ explanation, selectedText, mode });
  } catch (err) {
    console.error('Explain error:', err);
    res.status(500).json({ error: err.message });
  }
});

export default router;
