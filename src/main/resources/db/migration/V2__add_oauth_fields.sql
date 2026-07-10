ALTER TABLE users RENAME COLUMN google_access_token TO access_token;
ALTER TABLE users RENAME COLUMN google_refresh_token TO refresh_token;

ALTER TABLE users ADD COLUMN provider VARCHAR(50);
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);
ALTER TABLE users ADD COLUMN profile_picture_url TEXT;
ALTER TABLE users ADD COLUMN token_expires_at TIMESTAMPTZ;
