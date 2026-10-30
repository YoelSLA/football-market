-- add_catalog_normalized_name_search
-- La normalización pertenece solo a la búsqueda; player.name conserva su presentación.
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

CREATE FUNCTION public.catalog_normalize_name(input text)
RETURNS text
LANGUAGE sql
IMMUTABLE
STRICT
PARALLEL SAFE
SET search_path = pg_catalog
RETURN lower(regexp_replace(normalize(btrim(input), NFD), U&'[\0300-\036f]', '', 'g'));

CREATE INDEX ix_players_catalog_name_trgm
    ON public.players USING gin (public.catalog_normalize_name(name) public.gin_trgm_ops);
