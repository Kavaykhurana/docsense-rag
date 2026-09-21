# DocSense — Privacy‑Focused RAG Document Q&A Assistant

DocSense is a production‑quality, full‑stack **Retrieval‑Augmented Generation (RAG)**
application that lets you ask natural‑language questions about your own documents.
You sign in with Google, upload PDFs / DOCX / TXT / Markdown from your computer, and
DocSense extracts the text, splits it into chunks, generates vector embeddings with
Google Gemini, stores them in **PostgreSQL + pgvector**, and answers questions with
grounded responses and **source citations**.

> **Privacy model:** your original files always stay on your computer. The server only
> keeps the extracted text chunks and their embeddings that are needed to answer
> questions — never the uploaded file itself.

---

## Table of contents

- [Features](#features)
- [Architecture](#architecture)
- [RAG pipeline](#rag-pipeline)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Database schema](#database-schema)
- [Authentication](#authentication)
- [Environment variables](#environment-variables)
- [Local setup](#local-setup)
- [REST API](#rest-api)
- [Testing](#testing)
- [Deployment (Render)](#deployment-render)
- [Development status](#development-status)
- [Known limitations](#known-limitations)

---

## Features

- Google OAuth2 sign‑in with automatic account provisioning.
- Per‑user data isolation — a user can only ever see their own documents and chats.
- Upload and processing of **PDF, DOCX, TXT, and Markdown** files.
- Full document pipeline: validate → extract text → clean → chunk → embed → store.
- Semantic search inside PostgreSQL using **pgvector** (no external vector DB).
- Grounded question answering with **Google Gemini** via **Spring AI**.
- **Multi‑document** RAG (ask across one, several, or all of your documents).
- **Citations** that reference the source document and page where available.
- Persistent **chat history** stored in PostgreSQL.
- Document and conversation deletion (cascades remove derived RAG data).
- Clean, modern **light‑theme** UI (React + Vite + Tailwind CSS).

---

## Architecture

```
┌────────────────────┐        ┌───────────────────────────┐        ┌────────────────────┐
│  React + Vite SPA  │  HTTP  │   Spring Boot REST API    │  JPA   │  PostgreSQL +       │
│  (light theme)     │┌──────►│   Spring Security OAuth2  │┌──────►│  pgvector (Flyway)  │
│                    │◄───────│   Spring AI (Gemini)      │◄───────│                     │
└────────────────────┘  JSON  └─────────────┬─────────────┘        └─────────────────────┘
   user's own files                          │
   stay local                                │  HTTPS (server-side only; key never
                                             ▼   reaches the browser)
                                   ┌────────────────────┐
                                   │  Google Gemini API │
                                   │  chat + embeddings │
                                   └────────────────────┘
```

- **Frontend** (`/frontend`) — React SPA served by Vite; calls the backend on `/api/**`
  and starts login by navigating to `/oauth2/authorization/google`.
- **Backend** (`/backend`) — Spring Boot application implementing the REST API, OAuth2
  login, document processing, embeddings, retrieval, and Gemini answer generation.
- **Database** (`/database`) — PostgreSQL with the `pgvector` extension; schema managed
  by Flyway migrations in `backend/src/main/resources/db/migration`.

---

## RAG pipeline

```
Upload → Validation → Text Extraction → Text Cleaning → Chunking → Gemini Embeddings
   → PostgreSQL + pgvector → (query) Question Embedding → pgvector Top-K Similarity
   → Context Construction → Gemini Prompt → Grounded Answer + Citations
```

The LLM is instructed to answer **only** from retrieved context, to say so when the
answer is not present, to distinguish document content from general knowledge, and to
cite the source document/page. Page numbers are never fabricated.

---

## Tech stack

| Layer        | Technology                                                             |
|--------------|------------------------------------------------------------------------|
| Frontend     | React, Vite, Tailwind CSS (JavaScript), React Router, Axios           |
| Backend      | Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA / Hibernate, OAuth2 Client |
| AI           | Google Gemini (chat + embeddings) via Spring AI                        |
| Database     | PostgreSQL + pgvector, Flyway migrations                               |
| Build        | Maven (backend), npm/Vite (frontend)                                   |
| Deployment   | Render                                                                 |

---

## Project structure

```
.
├── backend/           # Spring Boot API, security, RAG services
│   └── src/main/resources/db/migration/   # Flyway SQL migrations
├── frontend/          # React + Vite + Tailwind SPA
├── database/          # Database bootstrap helper scripts / notes
├── docs/              # Additional documentation
├── .env.example       # Placeholder environment configuration
├── .gitignore
└── README.md
```

---

## Database schema

Created and versioned by Flyway (`V1__initial_schema.sql`).

- **users** — `id, google_id, email, name, profile_picture, created_at, updated_at`
- **documents** — `id, user_id→users, filename, file_type, file_size, status, page_count, created_at, updated_at`
- **document_chunks** — `id, document_id→documents, chunk_index, content, page_number, embedding vector(<dim>), created_at`
- **conversations** — `id, user_id→users, title, created_at, updated_at`
- **messages** — `id, conversation_id→conversations, role, content, created_at`

Foreign keys use `ON DELETE CASCADE` so deleting a document removes its chunks/vectors,
and deleting a user removes their documents and conversations. The `embedding` column
is a pgvector type with an HNSW cosine-similarity index. Its dimension is controlled by
the `EMBEDDING_DIMENSION` Flyway placeholder and must match the embedding model
(`text-embedding-004` → 768, `gemini-embedding-001` → 3072).

---

## Authentication

Google OAuth2 (Spring Security OAuth2 Client) using a cookie-based session:

1. The SPA navigates to `GET /oauth2/authorization/google`.
2. Google authenticates the user and redirects to `/login/oauth2/code/google`.
3. `CustomOAuth2UserService` creates/updates the local `users` row (minimum fields only).
4. The browser is redirected back to the SPA; subsequent `/api/**` calls are
   authenticated by the session cookie.

**CSRF:** the session cookie is `httpOnly` and `SameSite=Lax`, and CORS is restricted
to the frontend origin. Browsers do not send Lax cookies on cross-site subrequests,
which mitigates CSRF for this architecture; the OAuth2 handshake uses top-level
redirects. CSRF token enforcement is therefore intentionally disabled and documented.
Gemini credentials live only on the server and are never exposed to the frontend.

---

## Environment variables

Copy `.env.example` to `.env` and fill in real values. **Never commit `.env`.**

| Variable | Purpose |
|----------|---------|
| `PORT`, `FRONTEND_URL`, `ALLOWED_ORIGINS` | Server port and CORS/redirect configuration |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection |
| `EMBEDDING_DIMENSION` | pgvector column dimensionality (must match embedding model) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth2 client credentials |
| `GEMINI_API_KEY` | Google Gemini API key (backend only) |
| `GEMINI_CHAT_MODEL`, `GEMINI_EMBEDDING_MODEL` | Gemini model selection |
| `CHUNK_SIZE`, `CHUNK_OVERLAP`, `MAX_FILE_SIZE`, `MAX_FILE_SIZE_BYTES` | Document processing |

---

## Local setup

### Prerequisites

- **Java 21 (LTS)** — e.g. `brew install openjdk@21`
- **Maven 3.9+**
- **Node.js 18+** and npm
- **PostgreSQL 15+** with the **pgvector** extension (Render uses its own in production)

### 1. Database

```bash
# create the app role + database (adjust to your local Postgres setup)
psql -d postgres -c "CREATE ROLE docqa_app LOGIN PASSWORD 'docqa_app_pw' SUPERUSER;"
psql -d postgres -c "CREATE DATABASE doc_qa OWNER docqa_app;"
psql -d doc_qa -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

Flyway creates all tables on first backend start (no manual schema step needed).

### 2. Google OAuth2 credentials

1. Google Cloud Console → **APIs & Services → Credentials** → **Create OAuth client ID**
   (type *Web application*).
2. Add **Authorized redirect URI**: `http://localhost:8080/login/oauth2/code/google`
   (and your Render URL later).
3. Put the resulting Client ID / Secret into `.env`.

### 3. Gemini API key

Get a key from Google AI Studio and set `GEMINI_API_KEY` in `.env`.

### 4. Run the backend

```bash
cd backend
set -a && source ../.env && set +a        # load env vars
JAVA_HOME=/path/to/jdk-21 mvn spring-boot:run   # http://localhost:8080
```

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev                               # http://localhost:5173
```

The Vite dev server proxies `/api`, `/oauth2`, and `/login` to `http://localhost:8080`.

---

## REST API

All non-auth endpoints require an authenticated session; responses use DTOs (never JPA
entities).

### Authentication
| Method | Path | Description |
|--------|------|-------------|
| GET | `/oauth2/authorization/google` | Start Google login |
| GET | `/login/oauth2/code/google` | OAuth2 callback |
| GET | `/api/auth/me` | Current user profile |
| POST | `/api/auth/logout` | End session |

### Documents *(implemented in later phases)*
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/documents` | Upload + process a document |
| GET | `/api/documents` | List the user's documents |
| GET | `/api/documents/{id}` | Document details |
| DELETE | `/api/documents/{id}` | Delete document + its RAG data |

### Chat *(implemented in later phases)*
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/chat` | Ask a question (optionally scoped to documents) |
| GET | `/api/conversations` | List conversations |
| GET | `/api/conversations/{id}` | Conversation with messages |
| PATCH | `/api/conversations/{id}` | Rename conversation |
| DELETE | `/api/conversations/{id}` | Delete conversation |

---

## Testing

Automated tests (added in the testing phase) cover authentication and user isolation,
document upload/validation/processing/deletion, the RAG pipeline (chunking, embedding,
retrieval, context construction, answering), chat persistence and multi-document
retrieval, plus the end-to-end workflow.

Run with:

```bash
cd backend && JAVA_HOME=/path/to/jdk-21 mvn test
```

---

## Deployment (Render)

Target topology: React static site → Spring Boot web service → Render PostgreSQL
(with pgvector) → Gemini API. Environment variables, CORS, the Google OAuth2 redirect
URI, Flyway migrations, and production builds are configured in the Render dashboard.
Uploaded files are **never** persisted to Render's filesystem. Full step-by-step
instructions are finalized in the deployment phase.

---

## Development status

Built incrementally per the project plan:

- **Phase 1 — Setup, database, Google OAuth2:** ✅ complete and verified locally
  (backend builds & boots, Flyway migrates the pgvector schema, Hibernate validates it,
  `/api/auth/me` returns a clean 401 when unauthenticated, frontend builds with the
  light‑theme landing + dashboard). End-to-end Google login and Gemini calls require the
  real credentials listed above.
- **Phases 2–10** (upload/extraction, chunking/embeddings, retrieval, RAG answers,
  chat/multi-document, full UI, security hardening, tests, Render deployment): in progress.

## Known limitations

- Real Google login and Gemini-powered answers require valid credentials (see
  [Environment variables](#environment-variables)); these are provided by you and are
  never committed to the repository.
- `EMBEDDING_DIMENSION` must match the configured embedding model; changing models
  requires a matching new migration.
