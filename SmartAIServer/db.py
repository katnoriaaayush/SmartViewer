"""
SQLite persistence layer for the SmartAI server.

Uses the stdlib sqlite3 module with WAL mode and foreign keys enabled. A single
module-level connection is shared (check_same_thread=False) and all writes are
guarded by a lock — the workload is a single interactive whiteboard, so
contention is negligible.
"""
import logging
import sqlite3
import threading
from pathlib import Path

logger = logging.getLogger("smartai.db")

BASE_DIR = Path(__file__).resolve().parent
UPLOADS_DIR = BASE_DIR / "uploads"
UPLOADS_DIR.mkdir(exist_ok=True)
DB_PATH = BASE_DIR / "smartai.db"

_write_lock = threading.Lock()

_conn = sqlite3.connect(DB_PATH, check_same_thread=False)
_conn.row_factory = sqlite3.Row
_conn.execute("PRAGMA journal_mode = WAL")
_conn.execute("PRAGMA foreign_keys = ON")

_conn.executescript(
    """
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

    CREATE INDEX IF NOT EXISTS idx_chat_session    ON chat_messages(session_id, timestamp);
    CREATE INDEX IF NOT EXISTS idx_flashcards_doc  ON flashcards(document_id);
    CREATE INDEX IF NOT EXISTS idx_annotations_doc ON annotations(document_id);
    """
)
_conn.commit()
logger.info("Database ready at %s", DB_PATH)


# ── Documents ──────────────────────────────────────────────────────────────────

def get_document(doc_id: str):
    return _conn.execute("SELECT * FROM documents WHERE id = ?", (doc_id,)).fetchone()


def list_documents():
    return _conn.execute("SELECT * FROM documents ORDER BY uploaded_at DESC").fetchall()


def insert_document(doc_id, file_uri, session_id, file_name, mime_type, file_size):
    with _write_lock:
        _conn.execute(
            """INSERT OR IGNORE INTO documents
               (id, file_uri, session_id, file_name, mime_type, file_size)
               VALUES (?, ?, ?, ?, ?, ?)""",
            (doc_id, file_uri, session_id, file_name, mime_type, file_size),
        )
        _conn.commit()


def delete_document(doc_id: str):
    with _write_lock:
        _conn.execute("DELETE FROM documents WHERE id = ?", (doc_id,))
        _conn.commit()


# ── Chat ───────────────────────────────────────────────────────────────────────

def insert_message(session_id: str, role: str, content: str):
    with _write_lock:
        _conn.execute(
            "INSERT INTO chat_messages (session_id, role, content) VALUES (?, ?, ?)",
            (session_id, role, content),
        )
        _conn.commit()


def get_history(session_id: str):
    return _conn.execute(
        "SELECT * FROM chat_messages WHERE session_id = ? ORDER BY timestamp ASC LIMIT 20",
        (session_id,),
    ).fetchall()


def clear_history(session_id: str):
    with _write_lock:
        _conn.execute("DELETE FROM chat_messages WHERE session_id = ?", (session_id,))
        _conn.commit()


# ── Summaries ──────────────────────────────────────────────────────────────────

def get_summary(document_id: str, mode: str):
    return _conn.execute(
        "SELECT * FROM summaries WHERE document_id = ? AND mode = ?", (document_id, mode)
    ).fetchone()


def upsert_summary(document_id: str, mode: str, content: str):
    with _write_lock:
        _conn.execute(
            """INSERT INTO summaries (document_id, mode, content, cached_at)
               VALUES (?, ?, ?, unixepoch())
               ON CONFLICT(document_id, mode) DO UPDATE
                 SET content = excluded.content, cached_at = excluded.cached_at""",
            (document_id, mode, content),
        )
        _conn.commit()


# ── Flashcards ─────────────────────────────────────────────────────────────────

def get_flashcards(document_id: str):
    return _conn.execute(
        "SELECT * FROM flashcards WHERE document_id = ?", (document_id,)
    ).fetchall()


def replace_flashcards(document_id: str, cards: list[dict]):
    """Atomically delete existing cards for the document and insert the new set."""
    with _write_lock:
        try:
            _conn.execute("BEGIN")
            _conn.execute("DELETE FROM flashcards WHERE document_id = ?", (document_id,))
            _conn.executemany(
                """INSERT INTO flashcards (document_id, front, back, difficulty, page)
                   VALUES (?, ?, ?, ?, ?)""",
                [
                    (
                        document_id,
                        c["front"],
                        c["back"],
                        c["difficulty"],
                        c.get("page"),
                    )
                    for c in cards
                ],
            )
            _conn.commit()
        except Exception:
            _conn.rollback()
            raise


# ── Insights ───────────────────────────────────────────────────────────────────

def get_insights(document_id: str):
    return _conn.execute(
        "SELECT * FROM document_insights WHERE document_id = ?", (document_id,)
    ).fetchone()


def upsert_insights(document_id, key_points, entities, action_items, topics):
    with _write_lock:
        _conn.execute(
            """INSERT INTO document_insights
               (document_id, key_points, entities, action_items, topics, cached_at)
               VALUES (?, ?, ?, ?, ?, unixepoch())
               ON CONFLICT(document_id) DO UPDATE SET
                 key_points   = excluded.key_points,
                 entities     = excluded.entities,
                 action_items = excluded.action_items,
                 topics       = excluded.topics,
                 cached_at    = excluded.cached_at""",
            (document_id, key_points, entities, action_items, topics),
        )
        _conn.commit()


# ── Index ──────────────────────────────────────────────────────────────────────

def get_index(document_id: str):
    return _conn.execute(
        "SELECT * FROM document_indexes WHERE document_id = ?", (document_id,)
    ).fetchone()


def upsert_index(document_id: str, sections: str):
    with _write_lock:
        _conn.execute(
            """INSERT INTO document_indexes (document_id, sections, cached_at)
               VALUES (?, ?, unixepoch())
               ON CONFLICT(document_id) DO UPDATE
                 SET sections = excluded.sections, cached_at = excluded.cached_at""",
            (document_id, sections),
        )
        _conn.commit()


# ── Annotations ────────────────────────────────────────────────────────────────

def get_annotations(document_id: str):
    return _conn.execute(
        "SELECT * FROM annotations WHERE document_id = ? ORDER BY created_at DESC",
        (document_id,),
    ).fetchall()


def insert_annotation(document_id, text, page, color, note) -> int:
    with _write_lock:
        cur = _conn.execute(
            """INSERT INTO annotations (document_id, text, page, color, note)
               VALUES (?, ?, ?, ?, ?)""",
            (document_id, text, page, color, note),
        )
        _conn.commit()
        return cur.lastrowid


def delete_annotation(annotation_id, document_id) -> int:
    with _write_lock:
        cur = _conn.execute(
            "DELETE FROM annotations WHERE id = ? AND document_id = ?",
            (annotation_id, document_id),
        )
        _conn.commit()
        return cur.rowcount
