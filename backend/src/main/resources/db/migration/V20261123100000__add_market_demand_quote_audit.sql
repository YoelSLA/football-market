ALTER TABLE player_quotes
    ADD COLUMN sport_multiplier NUMERIC(18,6),
    ADD COLUMN market_demand_factor NUMERIC(18,6),
    ADD COLUMN market_multiplier NUMERIC(18,6),
    ADD COLUMN market_net_demand_tokens BIGINT;

ALTER TABLE player_quotes
    DROP CONSTRAINT ck_player_quote_origin,
    ADD CONSTRAINT ck_player_quote_origin
        CHECK (origin IN ('INITIAL', 'RECOMPUTED', 'CARRIED_FORWARD', 'MARKET_DEMAND')),
    ADD CONSTRAINT ck_player_quote_initial_market_audit
        CHECK (origin <> 'INITIAL' OR (
            sport_multiplier IS NULL
            AND market_demand_factor IS NULL
            AND market_multiplier IS NULL
            AND market_net_demand_tokens IS NULL
        )),
    ADD CONSTRAINT ck_player_quote_carried_market_audit
        CHECK (origin <> 'CARRIED_FORWARD' OR (
            sport_multiplier IS NULL
            AND market_demand_factor IS NULL
            AND market_multiplier IS NULL
            AND market_net_demand_tokens IS NULL
        )),
    ADD CONSTRAINT ck_player_quote_recomputed_market_audit
        CHECK (origin <> 'RECOMPUTED' OR (
            (sport_multiplier IS NULL
                AND market_demand_factor IS NULL
                AND market_multiplier IS NULL
                AND market_net_demand_tokens IS NULL)
            OR (sport_multiplier IS NOT NULL
                AND sport_multiplier > 0
                AND market_demand_factor IS NOT NULL
                AND market_multiplier IS NOT NULL
                AND market_demand_factor BETWEEN -0.50 AND 0.50
                AND market_multiplier = 1 + market_demand_factor
                AND market_net_demand_tokens IS NOT NULL)
        )),
    ADD CONSTRAINT ck_player_quote_market_demand_audit
        CHECK (origin <> 'MARKET_DEMAND' OR (
            score IS NULL
            AND strategy_name IS NULL
            AND strategy_version IS NULL
            AND source_valuation_date IS NULL
            AND sport_multiplier IS NULL
            AND market_demand_factor IS NOT NULL
            AND market_multiplier IS NOT NULL
            AND market_demand_factor BETWEEN -0.50 AND 0.50
            AND market_multiplier = 1 + market_demand_factor
            AND market_net_demand_tokens IS NOT NULL
        ));

ALTER TABLE quote_executions
    ADD COLUMN recalculation_type VARCHAR(32) NOT NULL DEFAULT 'SPORT_AND_MARKET',
    ADD COLUMN demand_window INTERVAL;

ALTER TABLE quote_executions
    ALTER COLUMN strategy_name DROP NOT NULL,
    ALTER COLUMN strategy_version DROP NOT NULL,
    ALTER COLUMN adjustment_factor DROP NOT NULL,
    ADD CONSTRAINT ck_quote_execution_recalculation_type
        CHECK (recalculation_type IN ('SPORT_AND_MARKET', 'MARKET_DEMAND_ONLY')),
    ADD CONSTRAINT ck_quote_execution_strategy_by_type
        CHECK (
            (recalculation_type = 'SPORT_AND_MARKET'
                AND strategy_name IS NOT NULL
                AND strategy_version IS NOT NULL
                AND adjustment_factor IS NOT NULL)
            OR (recalculation_type = 'MARKET_DEMAND_ONLY'
                AND strategy_name IS NULL
                AND strategy_version IS NULL
                AND adjustment_factor IS NULL
                AND demand_window IS NOT NULL
                AND demand_window > INTERVAL '0 seconds')
        ),
    ADD CONSTRAINT ck_quote_execution_demand_window
        CHECK (demand_window IS NULL OR demand_window > INTERVAL '0 seconds');
