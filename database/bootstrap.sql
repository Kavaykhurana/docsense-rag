-- Local PostgreSQL + pgvector bootstrap for DocSense.
-- Run against the default 'postgres' maintenance database, then create the app DB.
-- Adjust role/password/database to match your local setup and .env.

DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'docqa_app') THEN
    CREATE ROLE docqa_app LOGIN PASSWORD 'docqa_app_pw' SUPERUSER;
  END IF;
END $$;

-- Create the database (execute as a superuser):
--   CREATE DATABASE doc_qa OWNER docqa_app;
-- Then, connected to doc_qa, enable pgvector:
--   CREATE EXTENSION IF NOT EXISTS vector;
--
-- Note: Flyway also runs CREATE EXTENSION and applies the full schema on backend start.
-- This script is a convenience for a clean local machine.
