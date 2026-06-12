"""
Vertex AI (Gemini) client wrapper using the google-genai SDK.

The client is created lazily on first use so the server can start without
credentials present. Authentication uses Application Default Credentials —
set GOOGLE_APPLICATION_CREDENTIALS to the path of your service-account JSON.
"""
import json
import logging
import os
import time

from google import genai
from google.genai import types

logger = logging.getLogger("smartai.gemini")

MODEL = os.environ.get("VERTEX_MODEL", "gemini-2.0-flash-001")

_client: genai.Client | None = None


def client() -> genai.Client:
    """Lazily construct (and memoise) the Vertex AI client."""
    global _client
    if _client is None:
        project = os.environ.get("GOOGLE_CLOUD_PROJECT")
        location = os.environ.get("GOOGLE_CLOUD_LOCATION", "us-central1")
        logger.info(
            "Initialising Vertex AI client (project=%s, location=%s, model=%s)",
            project, location, MODEL,
        )
        _client = genai.Client(vertexai=True, project=project, location=location)
    return _client


def _file_part(file_path: str, mime_type: str) -> types.Part:
    with open(file_path, "rb") as fh:
        data = fh.read()
    return types.Part.from_bytes(data=data, mime_type=mime_type)


def _text(content: str) -> types.Part:
    return types.Part.from_text(text=content)


def _parse_json(raw: str) -> dict:
    """Extract the first JSON object from a model response, tolerating fences or trailing text."""
    start = raw.find("{")
    if start == -1:
        raise ValueError("No JSON object in model response")
    obj, _ = json.JSONDecoder().raw_decode(raw, start)
    return obj


# ── Chat (streaming) ────────────────────────────────────────────────────────────

async def chat_stream(file_path, mime_type, history, user_message):
    """
    Return an async iterator of GenerateContentResponse chunks.

    `history` is a list of rows with `role` ('user'|'model') and `content`.
    Uses the Chats API so the SDK handles history serialisation correctly.
    """
    history_contents: list[types.Content] = [
        types.Content(role=row["role"], parts=[_text(row["content"])])
        for row in history
    ]

    logger.info(
        "chat_stream → model=%s history_turns=%d msg_len=%d",
        MODEL, len(history), len(user_message),
    )

    chat = client().aio.chats.create(model=MODEL, history=history_contents)
    return await chat.send_message_stream(
        [_file_part(file_path, mime_type), _text(user_message)]
    )


# ── Summary ──────────────────────────────────────────────────────────────────────

async def summarize(file_path, mime_type, mode, opts=None) -> str:
    opts = opts or {}
    base_prompts = {
        "quick": "Provide a concise summary of this document in 150-200 words covering the main points.",
        "detailed": "Provide a comprehensive, detailed summary covering all major topics, key arguments, and conclusions.",
        "custom": opts.get("customPrompt") or "Summarize this document.",
    }
    prompt = base_prompts[mode]
    if opts.get("targetAudience"):
        prompt += f" Target audience: {opts['targetAudience']}."
    if opts.get("wordLimit"):
        prompt += f" Keep it under {opts['wordLimit']} words."

    started = time.monotonic()
    result = await client().aio.models.generate_content(
        model=MODEL,
        config=types.GenerateContentConfig(max_output_tokens=4096 if mode == "detailed" else 2048),
        contents=[types.Content(role="user", parts=[_file_part(file_path, mime_type), _text(prompt)])],
    )
    logger.info("summarize(mode=%s) done in %.2fs", mode, time.monotonic() - started)
    return result.text or ""


# ── Flashcards ────────────────────────────────────────────────────────────────────

async def generate_flashcards(file_path, mime_type, count=10) -> list[dict]:
    prompt = (
        f"Generate exactly {count} flashcards from this document.\n"
        "Return ONLY valid JSON, no markdown fences, no extra text:\n"
        '{"flashcards":[{"front":"question","back":"answer","difficulty":"easy|medium|hard","page":1}]}'
    )
    started = time.monotonic()
    result = await client().aio.models.generate_content(
        model=MODEL,
        config=types.GenerateContentConfig(max_output_tokens=4096, temperature=0.2),
        contents=[types.Content(role="user", parts=[_file_part(file_path, mime_type), _text(prompt)])],
    )
    logger.info("generate_flashcards(count=%d) done in %.2fs", count, time.monotonic() - started)
    return _parse_json(result.text or "").get("flashcards", [])


# ── Insights ──────────────────────────────────────────────────────────────────────

async def generate_insights(file_path, mime_type) -> dict:
    prompt = (
        "Analyse this document and return ONLY valid JSON, no markdown:\n"
        "{\n"
        '  "keyPoints":   ["point 1", "point 2"],\n'
        '  "entities":    [{"name": "Name", "type": "PERSON|ORGANIZATION|DATE|LOCATION|OTHER"}],\n'
        '  "actionItems": ["item 1"],\n'
        '  "topics":      ["topic 1"]\n'
        "}"
    )
    started = time.monotonic()
    result = await client().aio.models.generate_content(
        model=MODEL,
        config=types.GenerateContentConfig(max_output_tokens=4096, temperature=0.2),
        contents=[types.Content(role="user", parts=[_file_part(file_path, mime_type), _text(prompt)])],
    )
    logger.info("generate_insights done in %.2fs", time.monotonic() - started)
    return _parse_json(result.text or "")


# ── Index ─────────────────────────────────────────────────────────────────────────

async def generate_index(file_path, mime_type) -> list[dict]:
    prompt = (
        "Extract this document's table of contents / structure. Return ONLY valid JSON:\n"
        "{\n"
        '  "sections": [{"title": "Section Title", "page": 1, "summary": "one sentence", "level": 1}]\n'
        "}"
    )
    started = time.monotonic()
    result = await client().aio.models.generate_content(
        model=MODEL,
        config=types.GenerateContentConfig(max_output_tokens=2048, temperature=0.1),
        contents=[types.Content(role="user", parts=[_file_part(file_path, mime_type), _text(prompt)])],
    )
    logger.info("generate_index done in %.2fs", time.monotonic() - started)
    return _parse_json(result.text or "").get("sections", [])


# ── Explain ───────────────────────────────────────────────────────────────────────

async def explain(file_path, mime_type, selected_text, mode) -> str:
    instructions = {
        "explain": f'Explain the following text from the document clearly and thoroughly:\n"{selected_text}"',
        "simplify": f'Rewrite the following text in simple, easy-to-understand language:\n"{selected_text}"',
        "define": f'Define all technical terms, acronyms, and domain-specific concepts in:\n"{selected_text}"',
    }
    started = time.monotonic()
    result = await client().aio.models.generate_content(
        model=MODEL,
        config=types.GenerateContentConfig(max_output_tokens=1024),
        contents=[types.Content(role="user", parts=[_file_part(file_path, mime_type), _text(instructions[mode])])],
    )
    logger.info("explain(mode=%s) done in %.2fs", mode, time.monotonic() - started)
    return result.text or ""
