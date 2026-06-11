"""Streaming chat endpoint (Server-Sent Events)."""
import logging
from typing import Optional

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

import db
from gemini import chat_stream

logger = logging.getLogger("smartai.chat")

router = APIRouter()


class ChatRequest(BaseModel):
    message: str
    sessionId: Optional[str] = None


@router.post("/{doc_id}/chat")
async def chat(doc_id: str, body: ChatRequest):
    doc = db.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")
    if not body.message.strip():
        raise HTTPException(status_code=400, detail="message is required.")

    sid = body.sessionId or doc["session_id"]

    async def event_generator():
        full_response = ""
        try:
            history = db.get_history(sid)
            stream = await chat_stream(doc["file_uri"], doc["mime_type"], history, body.message)

            async for chunk in stream:
                token = getattr(chunk, "text", None) or ""
                if token:
                    full_response += token
                    # Preserve the existing SSE protocol the Android client expects
                    yield f"data: {token}\n\n"

            db.insert_message(sid, "user", body.message)
            db.insert_message(sid, "model", full_response)
            logger.info("chat session=%s streamed %d chars", sid[:8], len(full_response))

            yield "data: [DONE]\n\n"
        except Exception as err:  # noqa: BLE001 — surface any failure to the client
            logger.exception("Chat stream error for session %s", sid[:8])
            yield f"data: [ERROR] {err}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "Connection": "keep-alive"},
    )
