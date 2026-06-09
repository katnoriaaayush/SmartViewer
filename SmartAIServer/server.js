import 'dotenv/config';
import express from 'express';

import documentsRouter  from './routes/documents.js';
import chatRouter       from './routes/chat.js';
import aiFeaturesRouter from './routes/aifeatures.js';
import sessionsRouter   from './routes/sessions.js';

const app  = express();
const PORT = process.env.PORT || 3000;

app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true }));

// Permissive CORS for Android emulator (10.0.2.2) and LAN devices
app.use((_, res, next) => {
  res.setHeader('Access-Control-Allow-Origin',  '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  next();
});

app.options('*', (_, res) => res.sendStatus(204));

app.get('/health', (_, res) => {
  res.json({
    status:         'ok',
    uptime:         Math.floor(process.uptime()),
    vertexProject:  process.env.GOOGLE_CLOUD_PROJECT  || '(not set)',
    vertexModel:    process.env.VERTEX_MODEL           || '(not set)',
    vertexLocation: process.env.GOOGLE_CLOUD_LOCATION || '(not set)',
    credentials:    process.env.GOOGLE_APPLICATION_CREDENTIALS ? 'configured' : '(not set)',
  });
});

app.use('/api/documents', documentsRouter);
app.use('/api/documents', chatRouter);
app.use('/api/documents', aiFeaturesRouter);
app.use('/api/sessions',  sessionsRouter);

// Global error handler
app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: err.message || 'Internal server error' });
});

app.listen(PORT, () => {
  console.log(`SmartAI Server  →  http://localhost:${PORT}`);
  console.log(`Health check    →  http://localhost:${PORT}/health`);
  console.log(`Vertex project  →  ${process.env.GOOGLE_CLOUD_PROJECT || '(not set)'}`);
});
