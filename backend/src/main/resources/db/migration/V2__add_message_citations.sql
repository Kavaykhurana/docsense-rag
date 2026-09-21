-- V2: Persist source citations alongside assistant messages so that reloaded
-- chat history re-renders the exact document/page references (spec §6/§13).
ALTER TABLE messages ADD COLUMN citations jsonb;
