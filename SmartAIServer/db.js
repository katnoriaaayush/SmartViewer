import Database from 'better-sqlite3';
import { mkdirSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

mkdirSync(path.join(__dirname, 'uploads'), { recursive: true });

const db = new Database(path.join(__dirname, 'smartai.db'));

db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
  CREATE TABLE IF NOT EXISTS documents (
    id          TEXT PRIMARY KEY,
    file_uri    TEXT NOT NULL,
    session_id  TEXT NOT NULL,
    file_name   TEXT NOT NULL,
    mime_type   TEXT NOT NULL,
    file_size   INTEGER NOT NULL,
    uploaded_at INTEGER NOT NULL DEFAULT (unixepoch())
  );

  CREATE TABLE IF NOT EXISTS chat_messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id  TEXT NOT NULL,
    role        TEXT NOT NULL CHECK(role IN ('user','model')),
    content     TEXT NOT NULL,
    timestamp   INTEGER NOT NULL DEFAULT (unixepoch())
  );

  CREATE TABLE IF NOT EXISTS summaries (
    document_id TEXT NOT NULL,
    mode        TEXT NOT NULL CHECK(mode IN ('quick','detailed','custom')),
    content     TEXT NOT NULL,
    cached_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (document_id, mode)
  );

  CREATE TABLE IF NOT EXISTS flashcards (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id TEXT NOT NULL,
    front       TEXT NOT NULL,
    back        TEXT NOT NULL,
    difficulty  TEXT NOT NULL CHECK(difficulty IN ('easy','medium','hard')),
    page        INTEGER,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS document_insights (
    document_id  TEXT PRIMARY KEY,
    key_points   TEXT NOT NULL,
    entities     TEXT NOT NULL,
    action_items TEXT NOT NULL,
    topics       TEXT NOT NULL,
    cached_at    INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS document_indexes (
    document_id TEXT PRIMARY KEY,
    sections    TEXT NOT NULL,
    cached_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS annotations (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id TEXT NOT NULL,
    text        TEXT NOT NULL,
    page        INTEGER,
    color       TEXT NOT NULL DEFAULT '#FFFF00',
    note        TEXT,
    created_at  INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
  );

  CREATE INDEX IF NOT EXISTS idx_chat_session     ON chat_messages(session_id, timestamp);
  CREATE INDEX IF NOT EXISTS idx_flashcards_doc   ON flashcards(document_id);
  CREATE INDEX IF NOT EXISTS idx_annotations_doc  ON annotations(document_id);
`);

export const stmts = {
  // Documents
  insertDocument: db.prepare(`
    INSERT OR IGNORE INTO documents (id, file_uri, session_id, file_name, mime_type, file_size)
    VALUES (@id, @fileUri, @sessionId, @fileName, @mimeType, @fileSize)
  `),
  getDocument:    db.prepare(`SELECT * FROM documents WHERE id = ?`),
  listDocuments:  db.prepare(`SELECT * FROM documents ORDER BY uploaded_at DESC`),
  deleteDocument: db.prepare(`DELETE FROM documents WHERE id = ?`),

  // Chat
  insertMessage: db.prepare(`
    INSERT INTO chat_messages (session_id, role, content)
    VALUES (@sessionId, @role, @content)
  `),
  getHistory: db.prepare(`
    SELECT * FROM chat_messages WHERE session_id = ?
    ORDER BY timestamp ASC LIMIT 20
  `),
  clearHistory: db.prepare(`DELETE FROM chat_messages WHERE session_id = ?`),

  // Summaries
  getSummary: db.prepare(`SELECT * FROM summaries WHERE document_id = ? AND mode = ?`),
  upsertSummary: db.prepare(`
    INSERT INTO summaries (document_id, mode, content, cached_at)
    VALUES (@documentId, @mode, @content, unixepoch())
    ON CONFLICT(document_id, mode) DO UPDATE
      SET content = excluded.content, cached_at = excluded.cached_at
  `),

  // Flashcards
  getFlashcards:    db.prepare(`SELECT * FROM flashcards WHERE document_id = ?`),
  deleteFlashcards: db.prepare(`DELETE FROM flashcards WHERE document_id = ?`),
  insertFlashcard:  db.prepare(`
    INSERT INTO flashcards (document_id, front, back, difficulty, page)
    VALUES (@documentId, @front, @back, @difficulty, @page)
  `),

  // Insights
  getInsights: db.prepare(`SELECT * FROM document_insights WHERE document_id = ?`),
  upsertInsights: db.prepare(`
    INSERT INTO document_insights (document_id, key_points, entities, action_items, topics, cached_at)
    VALUES (@documentId, @keyPoints, @entities, @actionItems, @topics, unixepoch())
    ON CONFLICT(document_id) DO UPDATE SET
      key_points   = excluded.key_points,
      entities     = excluded.entities,
      action_items = excluded.action_items,
      topics       = excluded.topics,
      cached_at    = excluded.cached_at
  `),

  // Index
  getIndex: db.prepare(`SELECT * FROM document_indexes WHERE document_id = ?`),
  upsertIndex: db.prepare(`
    INSERT INTO document_indexes (document_id, sections, cached_at)
    VALUES (@documentId, @sections, unixepoch())
    ON CONFLICT(document_id) DO UPDATE
      SET sections = excluded.sections, cached_at = excluded.cached_at
  `),

  // Annotations
  getAnnotations: db.prepare(`
    SELECT * FROM annotations WHERE document_id = ? ORDER BY created_at DESC
  `),
  insertAnnotation: db.prepare(`
    INSERT INTO annotations (document_id, text, page, color, note)
    VALUES (@documentId, @text, @page, @color, @note)
  `),
  deleteAnnotation: db.prepare(`
    DELETE FROM annotations WHERE id = ? AND document_id = ?
  `),
};

export const insertFlashcardsBatch = db.transaction((documentId, cards) => {
  stmts.deleteFlashcards.run(documentId);
  for (const card of cards) {
    stmts.insertFlashcard.run({
      documentId,
      front:      card.front,
      back:       card.back,
      difficulty: card.difficulty,
      page:       card.page ?? null,
    });
  }
});

export default db;
