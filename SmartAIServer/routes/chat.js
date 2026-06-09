import { Router } from 'express';
import { stmts }  from '../db.js';
import { chatStream } from '../gemini.js';

const router = Router();

// POST /api/documents/:id/chat  — SSE streaming response
router.post('/:id/chat', async (req, res) => {
  const doc = stmts.getDocument.get(req.params.id);
  if (!doc) return res.status(404).json({ error: 'Document not found.' });

  const { message, sessionId } = req.body;
  if (!message?.trim()) return res.status(400).json({ error: 'message is required.' });

  const sid = sessionId || doc.session_id;

  res.setHeader('Content-Type',  'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection',    'keep-alive');
  res.flushHeaders();

  try {
    const history = stmts.getHistory.all(sid);
    const stream  = await chatStream(doc.file_uri, doc.mime_type, history, message);

    let fullResponse = '';
    for await (const chunk of stream) {
      const token = chunk.text ?? '';
      if (token) {
        fullResponse += token;
        res.write(`data: ${token}\n\n`);
      }
    }

    stmts.insertMessage.run({ sessionId: sid, role: 'user',  content: message });
    stmts.insertMessage.run({ sessionId: sid, role: 'model', content: fullResponse });

    res.write('data: [DONE]\n\n');
  } catch (err) {
    console.error('Chat stream error:', err);
    res.write(`data: [ERROR] ${err.message}\n\n`);
  } finally {
    res.end();
  }
});

export default router;
