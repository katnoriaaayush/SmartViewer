import { Router }                              from 'express';
import multer                                  from 'multer';
import { createHash, randomUUID }              from 'crypto';
import { writeFileSync, existsSync, mkdirSync } from 'fs';
import path                                    from 'path';
import { fileURLToPath }                       from 'url';
import { stmts }                               from '../db.js';

const __dirname  = path.dirname(fileURLToPath(import.meta.url));
const UPLOADS    = path.join(__dirname, '..', 'uploads');

const ALLOWED_TYPES = new Set([
  'application/pdf',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
]);

const upload = multer({
  storage: multer.memoryStorage(),
  limits:  { fileSize: 50 * 1024 * 1024 },
  fileFilter: (_, file, cb) => cb(null, ALLOWED_TYPES.has(file.mimetype)),
});

const router = Router();

// POST /api/documents/upload
router.post('/upload', upload.single('file'), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: 'No file uploaded or unsupported file type.' });
  }

  const { originalname, mimetype, size, buffer } = req.file;

  // SHA-256 content-addressed storage — same bytes → same ID, no re-upload
  const hash   = createHash('sha256').update(buffer).digest('hex');
  const subDir = path.join(UPLOADS, hash.slice(0, 2));
  const fileUri = path.join(subDir, hash);

  if (!existsSync(fileUri)) {
    mkdirSync(subDir, { recursive: true });
    writeFileSync(fileUri, buffer);
  }

  const existing  = stmts.getDocument.get(hash);
  const sessionId = existing?.session_id ?? randomUUID();

  stmts.insertDocument.run({
    id: hash, fileUri, sessionId,
    fileName: originalname, mimeType: mimetype, fileSize: size,
  });

  const doc = stmts.getDocument.get(hash);
  res.json({
    documentId: doc.id,
    sessionId:  doc.session_id,
    fileName:   doc.file_name,
    mimeType:   doc.mime_type,
    fileSize:   doc.file_size,
  });
});

// GET /api/documents
router.get('/', (_, res) => {
  res.json(stmts.listDocuments.all().map(d => ({
    documentId: d.id,
    sessionId:  d.session_id,
    fileName:   d.file_name,
    mimeType:   d.mime_type,
    fileSize:   d.file_size,
    uploadedAt: d.uploaded_at,
  })));
});

// DELETE /api/documents/:id
router.delete('/:id', (req, res) => {
  if (!stmts.getDocument.get(req.params.id)) {
    return res.status(404).json({ error: 'Document not found.' });
  }
  stmts.deleteDocument.run(req.params.id);
  res.json({ deleted: true });
});

// GET /api/documents/:id/annotations
router.get('/:id/annotations', (req, res) => {
  if (!stmts.getDocument.get(req.params.id)) {
    return res.status(404).json({ error: 'Document not found.' });
  }
  res.json(stmts.getAnnotations.all(req.params.id).map(a => ({
    id: a.id, text: a.text, page: a.page, color: a.color, note: a.note,
  })));
});

// POST /api/documents/:id/annotations
router.post('/:id/annotations', (req, res) => {
  if (!stmts.getDocument.get(req.params.id)) {
    return res.status(404).json({ error: 'Document not found.' });
  }
  const { text, page, color = '#FFFF00', note } = req.body;
  if (!text?.trim()) return res.status(400).json({ error: 'text is required.' });

  const result = stmts.insertAnnotation.run({
    documentId: req.params.id, text, page: page ?? null, color, note: note ?? null,
  });
  res.status(201).json({ id: result.lastInsertRowid, text, page, color, note });
});

// DELETE /api/documents/:id/annotations/:annId
router.delete('/:id/annotations/:annId', (req, res) => {
  const result = stmts.deleteAnnotation.run(req.params.annId, req.params.id);
  if (result.changes === 0) return res.status(404).json({ error: 'Annotation not found.' });
  res.json({ deleted: true });
});

export default router;
