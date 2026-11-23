ALTER TABLE market_transactions
    ADD COLUMN quote_price BIGINT,
    ADD COLUMN discount_percentage SMALLINT;

ALTER TABLE market_transactions
    ADD CONSTRAINT ck_market_transactions_quote_price
        CHECK (quote_price IS NULL OR quote_price > 0),
    ADD CONSTRAINT ck_market_transactions_discount_percentage
        CHECK (discount_percentage IS NULL OR discount_percentage BETWEEN 0 AND 100);
