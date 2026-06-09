import { Router } from 'express';
import { stmts }  from '../db.js';

const router = Router();

// GET /api/sessions/:id/history
router.get('/:id/history', (req, res) => {
  const messages = stmts.getHistory.all(req.params.id);
  res.json({
    messages: messages.map(m => ({
      id:        m.id,
      role:      m.role,
      content:   m.content,
      timestamp: m.timestamp,
    })),
  });
});

// DELETE /api/sessions/:id/history
router.delete('/:id/history', (req, res) => {
  stmts.clearHistory.run(req.params.id);
  res.json({ cleared: true });
});

export default router;
