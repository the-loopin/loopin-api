CREATE DOMAIN IF NOT EXISTS vector AS DOUBLE ARRAY;

CREATE TABLE IF NOT EXISTS event_embeddings (
  event_id BIGINT PRIMARY KEY,
  embedding vector NOT NULL,
  embedding_model VARCHAR(255) NOT NULL,
  source_text_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_interest_embeddings (
  user_id BIGINT PRIMARY KEY,
  embedding vector NOT NULL,
  embedding_model VARCHAR(255) NOT NULL,
  source_text_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
