"""Document upload / listing / deletion and annotation CRUD."""
import hashlib
import logging
import uuid
from typing import Optional

from fastapi import APIRouter, File, HTTPException, UploadFile
from pydantic import BaseModel

import db

logger = logging.getLogger("smartai.documents")

router = APIRouter()

ALLOWED_TYPES = {
    "application/pdf",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
}
MAX_FILE_SIZE = 50 * 1024 * 1024  # 50 MB


@router.post("/upload")
async def upload_document(file: UploadFile = File(...)):
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(status_code=400, detail="Unsupported file type.")

    buffer = await file.read()
    if not buffer:
        raise HTTPException(status_code=400, detail="No file uploaded.")
    if len(buffer) > MAX_FILE_SIZE:
        raise HTTPException(status_code=400, detail="File exceeds 50 MB limit.")

    # SHA-256 content-addressed storage — same bytes → same ID, no re-upload
    file_hash = hashlib.sha256(buffer).hexdigest()
    sub_dir = db.UPLOADS_DIR / file_hash[:2]
    file_uri = sub_dir / file_hash

    if not file_uri.exists():
        sub_dir.mkdir(parents=True, exist_ok=True)
        file_uri.write_bytes(buffer)
        logger.info("Stored new upload %s (%d bytes)", file_hash[:12], len(buffer))
    else:
        logger.info("Upload %s already on disk — reusing", file_hash[:12])

    existing = db.get_document(file_hash)
    session_id = existing["session_id"] if existing else str(uuid.uuid4())

    db.insert_document(
        doc_id=file_hash,
        file_uri=str(file_uri),
        session_id=session_id,
        file_name=file.filename or "document.pdf",
        mime_type=file.content_type,
        file_size=len(buffer),
    )

    doc = db.get_document(file_hash)
    return {
        "documentId": doc["id"],
        "sessionId": doc["session_id"],
        "fileName": doc["file_name"],
        "mimeType": doc["mime_type"],
        "fileSize": doc["file_size"],
    }


@router.get("")
@router.get("/")
async def list_documents():
    return [
        {
            "documentId": d["id"],
            "sessionId": d["session_id"],
            "fileName": d["file_name"],
            "mimeType": d["mime_type"],
            "fileSize": d["file_size"],
            "uploadedAt": d["uploaded_at"],
        }
        for d in db.list_documents()
    ]


@router.delete("/{doc_id}")
async def delete_document(doc_id: str):
    if not db.get_document(doc_id):
        raise HTTPException(status_code=404, detail="Document not found.")
    db.delete_document(doc_id)
    logger.info("Deleted document %s", doc_id[:12])
    return {"deleted": True}


# ── Annotations ──────────────────────────────────────────────────────────────

class AnnotationRequest(BaseModel):
    text: str
    page: Optional[int] = None
    color: str = "#FFFF00"
    note: Optional[str] = None


@router.get("/{doc_id}/annotations")
async def list_annotations(doc_id: str):
    if not db.get_document(doc_id):
        raise HTTPException(status_code=404, detail="Document not found.")
    return [
        {"id": a["id"], "text": a["text"], "page": a["page"], "color": a["color"], "note": a["note"]}
        for a in db.get_annotations(doc_id)
    ]


@router.post("/{doc_id}/annotations", status_code=201)
async def create_annotation(doc_id: str, body: AnnotationRequest):
    if not db.get_document(doc_id):
        raise HTTPException(status_code=404, detail="Document not found.")
    if not body.text.strip():
        raise HTTPException(status_code=400, detail="text is required.")

    new_id = db.insert_annotation(doc_id, body.text, body.page, body.color, body.note)
    return {"id": new_id, "text": body.text, "page": body.page, "color": body.color, "note": body.note}


@router.delete("/{doc_id}/annotations/{ann_id}")
async def delete_annotation(doc_id: str, ann_id: int):
    changes = db.delete_annotation(ann_id, doc_id)
    if changes == 0:
        raise HTTPException(status_code=404, detail="Annotation not found.")
    return {"deleted": True}
