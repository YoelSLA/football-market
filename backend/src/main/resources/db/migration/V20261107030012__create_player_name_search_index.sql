CREATE INDEX ix_players_catalog_name_trgm
    ON public.players USING gin (public.catalog_normalize_name(name) public.gin_trgm_ops);
