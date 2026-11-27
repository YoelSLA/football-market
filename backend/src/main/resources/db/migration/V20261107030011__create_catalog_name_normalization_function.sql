CREATE FUNCTION public.catalog_normalize_name(input text)
RETURNS text
LANGUAGE sql
IMMUTABLE
STRICT
PARALLEL SAFE
SET search_path = pg_catalog
RETURN lower(regexp_replace(normalize(btrim(input), NFD), U&'[\0300-\036f]', '', 'g'));
