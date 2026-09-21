-- V1: Initial schema for DocSense RAG document Q&A
-- Enables pgvector and creates the normalized relational model described in the spec (§10).
-- The embedding dimension is injected by Flyway placeholder ${embedding_dim} (see application.yml).

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    google_id       VARCHAR(255)    NOT NULL UNIQUE,
    email           VARCHAR(320)    NOT NULL UNIQUE,
    name            VARCHAR(255),
    profile_picture TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE documents (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename        VARCHAR(512)    NOT NULL,
    file_type       VARCHAR(16)     NOT NULL,
    file_size       BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    page_count      INTEGER,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_documents_file_type CHECK (file_type IN ('PDF', 'DOCX', 'TXT', 'MD')),
    CONSTRAINT chk_documents_status    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_documents_size      CHECK (file_size > 0)
);

CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_status  ON documents(status);

CREATE TABLE document_chunks (
    id           BIGSERIAL       PRIMARY KEY,
    document_id  BIGINT          NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index  INTEGER         NOT NULL,
    content      TEXT            NOT NULL,
    page_number  INTEGER,
    embedding    vector(${embedding_dim}),
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_chunks_index CHECK (chunk_index >= 0)
);

CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);

CREATE INDEX idx_chunks_embedding_hnsw
    ON document_chunks USING hnsw (embedding vector_cosine_ops);

CREATE TABLE conversations (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(512)    NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_user_id ON conversations(user_id);

CREATE TABLE messages (
    id              BIGSERIAL       PRIMARY KEY,
    conversation_id BIGINT          NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(16)     NOT NULL,
    content         TEXT            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_messages_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
