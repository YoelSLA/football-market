ALTER TABLE market_transactions
    ADD COLUMN listing_id BIGINT REFERENCES token_listings(id) ON DELETE NO ACTION;
