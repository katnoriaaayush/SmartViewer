"""Chat session history endpoints."""
import logging

from fastapi import APIRouter

import db

logger = logging.getLogger("smartai.sessions")

router = APIRouter()


@router.get("/{session_id}/history")
async def get_history(session_id: str):
    messages = db.get_history(session_id)
    return {
        "messages": [
            {"id": m["id"], "role": m["role"], "content": m["content"], "timestamp": m["timestamp"]}
            for m in messages
        ]
    }


@router.delete("/{session_id}/history")
async def clear_history(session_id: str):
    db.clear_history(session_id)
    logger.info("Cleared history for session %s", session_id[:8])
    return {"cleared": True}
