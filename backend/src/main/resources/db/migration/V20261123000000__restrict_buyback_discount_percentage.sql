ALTER TABLE market_transactions
    DROP CONSTRAINT ck_market_transactions_discount_percentage;

ALTER TABLE market_transactions
    ADD CONSTRAINT ck_market_transactions_discount_percentage
        CHECK (discount_percentage IS NULL OR discount_percentage BETWEEN 10 AND 99);
