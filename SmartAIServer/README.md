# SmartAI Server (Python / FastAPI)

AI backend for SmartViewer — FastAPI + Vertex AI (Gemini) + SQLite.

## Setup

```bash
cd SmartAIServer
python3 -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env               # then fill in the values
```

### `.env`

| Variable                         | Purpose                                              |
| -------------------------------- | ---------------------------------------------------- |
| `GOOGLE_APPLICATION_CREDENTIALS` | Absolute path to your service-account JSON           |
| `GOOGLE_CLOUD_PROJECT`           | GCP project id                                       |
| `GOOGLE_CLOUD_LOCATION`          | Vertex region (e.g. `us-central1`)                   |
| `VERTEX_MODEL`                   | Gemini model id (default `gemini-2.0-flash-001`)     |
| `PORT`                           | HTTP port (default `3000`)                           |
| `LOG_LEVEL`                      | `DEBUG` / `INFO` / `WARNING` (default `INFO`)        |

The service account needs the **Vertex AI User** role (`roles/aiplatform.user`).

## Run

```bash
python main.py                     # honours PORT
# or
uvicorn main:app --host 0.0.0.0 --port 3000 --reload
```

Verify: `curl http://localhost:3000/health`

## Logging

Logs stream to stdout and to `logs/server.log` (rotating, 5 × 2 MB). Set
`LOG_LEVEL=DEBUG` in `.env` to see cache hit/miss and per-request timing.

## API

| Method   | Path                                       | Purpose                    |
| -------- | ------------------------------------------ | -------------------------- |
| `POST`   | `/api/documents/upload`                    | Upload a PDF (multipart)   |
| `GET`    | `/api/documents`                           | List documents             |
| `DELETE` | `/api/documents/{id}`                      | Delete a document          |
| `POST`   | `/api/documents/{id}/chat`                 | Streaming chat (SSE)       |
| `POST`   | `/api/documents/{id}/summarize`            | Summary (quick/detailed/custom) |
| `POST`   | `/api/documents/{id}/flashcards`           | Generate flashcards        |
| `GET`    | `/api/documents/{id}/insights`             | Key points / entities      |
| `GET`    | `/api/documents/{id}/index`                | Document structure         |
| `POST`   | `/api/documents/{id}/explain`              | Explain selected text      |
| `GET`    | `/api/sessions/{id}/history`               | Chat history               |
| `DELETE` | `/api/sessions/{id}/history`               | Clear chat history         |
