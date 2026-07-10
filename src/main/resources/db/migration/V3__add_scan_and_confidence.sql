-- users: tarama zamanı takibi
ALTER TABLE users ADD COLUMN last_scan_at TIMESTAMPTZ;

-- mail_expectations: tek esnek senderIdentifier alanı (senderEmail/senderName'in yerini alır)
ALTER TABLE mail_expectations ADD COLUMN sender_identifier VARCHAR(500);
UPDATE mail_expectations
SET sender_identifier = TRIM(COALESCE(sender_name, '') || ' ' || COALESCE(sender_email, ''))
WHERE sender_name IS NOT NULL OR sender_email IS NOT NULL;
ALTER TABLE mail_expectations DROP COLUMN IF EXISTS sender_email;
ALTER TABLE mail_expectations DROP COLUMN IF EXISTS sender_name;

-- mail_matches: yeni alanlar
ALTER TABLE mail_matches ADD COLUMN from_email VARCHAR(255);
ALTER TABLE mail_matches ADD COLUMN confidence_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE mail_matches ADD COLUMN match_summary TEXT;
ALTER TABLE mail_matches ADD COLUMN dismissed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE mail_matches ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- mail_matches: gmail_message_id tek başına unique olamaz (aynı mail birden fazla beklentiyle eşleşebilir)
ALTER TABLE mail_matches DROP CONSTRAINT IF EXISTS mail_matches_gmail_message_id_key;
ALTER TABLE mail_matches ADD CONSTRAINT uq_mail_matches_msg_exp UNIQUE (gmail_message_id, expectation_id);

-- mail_match_sender_tokens: gönderen token eşleşme tablosu (@ElementCollection)
CREATE TABLE mail_match_sender_tokens (
    match_id UUID    NOT NULL REFERENCES mail_matches(id) ON DELETE CASCADE,
    token    VARCHAR(255)
);

CREATE INDEX idx_mail_match_sender_tokens_match_id ON mail_match_sender_tokens(match_id);
