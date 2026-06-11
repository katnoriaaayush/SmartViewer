"""
SmartAI Server — FastAPI + Vertex AI (Gemini) + SQLite.

Run with:  python main.py     (honours PORT, default 3000)
       or:  uvicorn main:app --host 0.0.0.0 --port 3000
"""
import logging
import os
import time

from dotenv import load_dotenv

# Load .env then configure logging before anything else imports a logger
load_dotenv()
import logging_config  # noqa: E402

logging_config.configure()

from contextlib import asynccontextmanager  # noqa: E402

from fastapi import FastAPI  # noqa: E402
from fastapi.middleware.cors import CORSMiddleware  # noqa: E402

from routes import aifeatures, chat, documents, sessions  # noqa: E402

logger = logging.getLogger("smartai")

START_TIME = time.time()


@asynccontextmanager
async def lifespan(_: "FastAPI"):
    port = os.environ.get("PORT", "3000")
    logger.info("SmartAI Server ready  →  http://localhost:%s", port)
    logger.info("Health check          →  http://localhost:%s/health", port)
    logger.info("Vertex project        →  %s", os.environ.get("GOOGLE_CLOUD_PROJECT", "(not set)"))
    yield


app = FastAPI(title="SmartAI Server", version="2.0.0", lifespan=lifespan)

# Permissive CORS for the Android emulator (10.0.2.2) and LAN devices
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

app.include_router(documents.router, prefix="/api/documents", tags=["documents"])
app.include_router(chat.router, prefix="/api/documents", tags=["chat"])
app.include_router(aifeatures.router, prefix="/api/documents", tags=["ai"])
app.include_router(sessions.router, prefix="/api/sessions", tags=["sessions"])


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "uptime": int(time.time() - START_TIME),
        "vertexProject": os.environ.get("GOOGLE_CLOUD_PROJECT", "(not set)"),
        "vertexModel": os.environ.get("VERTEX_MODEL", "(not set)"),
        "vertexLocation": os.environ.get("GOOGLE_CLOUD_LOCATION", "(not set)"),
        "credentials": "configured" if os.environ.get("GOOGLE_APPLICATION_CREDENTIALS") else "(not set)",
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "3000")))
