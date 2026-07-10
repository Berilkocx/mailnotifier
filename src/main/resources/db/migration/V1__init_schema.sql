CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    google_access_token TEXT,
    google_refresh_token TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE mail_expectations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_email VARCHAR(255),
    sender_name VARCHAR(255),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    matched_at TIMESTAMPTZ
);

CREATE TABLE mail_expectation_keywords (
    expectation_id UUID NOT NULL REFERENCES mail_expectations(id) ON DELETE CASCADE,
    keyword VARCHAR(255) NOT NULL
);

CREATE TABLE mail_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expectation_id UUID NOT NULL REFERENCES mail_expectations(id) ON DELETE CASCADE,
    gmail_message_id VARCHAR(255) NOT NULL UNIQUE,
    from_address VARCHAR(255),
    subject TEXT,
    snippet TEXT,
    match_score DOUBLE PRECISION,
    received_at TIMESTAMPTZ
);

CREATE TABLE mail_match_keywords (
    match_id UUID NOT NULL REFERENCES mail_matches(id) ON DELETE CASCADE,
    keyword VARCHAR(255) NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id UUID NOT NULL REFERENCES mail_matches(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mail_expectations_user_id ON mail_expectations(user_id);
CREATE INDEX idx_mail_expectations_is_active ON mail_expectations(is_active);
CREATE INDEX idx_mail_matches_expectation_id ON mail_matches(expectation_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
