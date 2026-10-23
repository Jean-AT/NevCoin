ALTER TABLE social_events
    ADD COLUMN entities_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN token_associations_json TEXT NOT NULL DEFAULT '[]';
