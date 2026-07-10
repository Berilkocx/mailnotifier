ALTER TABLE mail_matches ADD COLUMN content_summary TEXT;
ALTER TABLE mail_matches ADD COLUMN detected_intent VARCHAR(50);

CREATE TABLE mail_match_ai_topics (
    match_id UUID NOT NULL REFERENCES mail_matches(id) ON DELETE CASCADE,
    topic    VARCHAR(255)
);

CREATE INDEX idx_mail_match_ai_topics_match_id ON mail_match_ai_topics(match_id);
