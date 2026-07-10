-- notifications: confidence_level alanı
ALTER TABLE notifications ADD COLUMN confidence_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';

-- WEBSOCKET enum değeri IN_APP olarak güncellendi
UPDATE notifications SET type = 'IN_APP' WHERE type = 'WEBSOCKET';
