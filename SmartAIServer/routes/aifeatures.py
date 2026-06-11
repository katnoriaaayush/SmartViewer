"""AI feature endpoints: summarize, flashcards, insights, index, explain."""
import json
import logging
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

import db
import gemini

logger = logging.getLogger("smartai.aifeatures")

router = APIRouter()


# ── Summarize ────────────────────────────────────────────────────────────────

class SummarizeRequest(BaseModel):
    mode: str = "quick"
    customPrompt: Optional[str] = None
    targetAudience: Optional[str] = None
    wordLimit: Optional[int] = None


@router.post("/{doc_id}/summarize")
async def summarize(doc_id: str, body: SummarizeRequest):
    doc = db.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")

    valid_modes = {"quick", "detailed", "custom"}
    if body.mode not in valid_modes:
        raise HTTPException(status_code=400, detail=f"mode must be one of: {', '.join(valid_modes)}")

    if body.mode != "custom":
        cached = db.get_summary(doc_id, body.mode)
        if cached:
            logger.debug("summarize cache HIT doc=%s mode=%s", doc_id[:12], body.mode)
            return {"summary": cached["content"], "mode": body.mode, "cached": True}

    logger.debug("summarize cache MISS doc=%s mode=%s", doc_id[:12], body.mode)
    content = await gemini.summarize(
        doc["file_uri"], doc["mime_type"], body.mode,
        {"customPrompt": body.customPrompt, "targetAudience": body.targetAudience, "wordLimit": body.wordLimit},
    )
    if body.mode != "custom":
        db.upsert_summary(doc_id, body.mode, content)
    return {"summary": content, "mode": body.mode, "cached": False}


# ── Flashcards ───────────────────────────────────────────────────────────────

class FlashcardsRequest(BaseModel):
    count: int = 10


@router.post("/{doc_id}/flashcards")
async def flashcards(doc_id: str, body: FlashcardsRequest):
    doc = db.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")

    cached = db.get_flashcards(doc_id)
    if cached:
        logger.debug("flashcards cache HIT doc=%s (%d cards)", doc_id[:12], len(cached))
        return {
            "flashcards": [
                {"id": c["id"], "front": c["front"], "back": c["back"],
                 "difficulty": c["difficulty"], "page": c["page"]}
                for c in cached
            ],
            "cached": True,
        }

    count = min(body.count or 10, 20)
    logger.debug("flashcards cache MISS doc=%s count=%d", doc_id[:12], count)
    cards = await gemini.generate_flashcards(doc["file_uri"], doc["mime_type"], count)
    db.replace_flashcards(doc_id, cards)
    return {"flashcards": cards, "cached": False}


# ── Insights ─────────────────────────────────────────────────────────────────

@router.get("/{doc_id}/insights")
async def insights(doc_id: str):
    doc = db.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")

    cached = db.get_insights(doc_id)
    if cached:
        logger.debug("insights cache HIT doc=%s", doc_id[:12])
        return {
            "keyPoints": json.loads(cached["key_points"]),
            "entities": json.loads(cached["entities"]),
            "actionItems": json.loads(cached["action_items"]),
            "topics": json.loads(cached["topics"]),
            "cached": True,
        }

    logger.debug("insights cache MISS doc=%s", doc_id[:12])
    data = await gemini.generate_insights(doc["file_uri"], doc["mime_type"])
    db.upsert_insights(
        doc_id,
        json.dumps(data.get("keyPoints", [])),
        json.dumps(data.get("entities", [])),
        json.dumps(data.get("actionItems", [])),
        json.dumps(data.get("topics", [])),
    )
    return {**data, "cached": False}


# ── Index ────────────────────────────────────────────────────────────────────

@router.get("/{doc_id}/index")
async def index(doc_id: str):
    doc = db.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")

    cached = db.get_index(doc_id)
    if cached:
        logger.debug("index cache HIT doc=%s", doc_id[:12])
        return {"sections": json.loads(cached["sections"]), "cached": True}

    logger.debug("index cache MISS doc=%s", doc_id[:12])
    sections = await gemini.generate_index(doc["file_uri"], doc["mime_type"])
    db.upsert_index(doc_id, json.dumps(sections))
    return {"sections": sections, "cached": False}


# ── Explain ──────────────────────────────────────────────────────────────────

class ExplainRequest(BaseModel):
    selectedText: str
    mode: str = "explain"


@router.post("/{doc_id}/explain")
async def explain(doc_id: str, body: ExplainRequest):
    doc = db.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")
    if not body.selectedText.strip():
        raise HTTPException(status_code=400, detail="selectedText is required.")

    valid_modes = {"explain", "simplify", "define"}
    if body.mode not in valid_modes:
        raise HTTPException(status_code=400, detail=f"mode must be one of: {', '.join(valid_modes)}")

    explanation = await gemini.explain(doc["file_uri"], doc["mime_type"], body.selectedText, body.mode)
    return {"explanation": explanation, "selectedText": body.selectedText, "mode": body.mode}
