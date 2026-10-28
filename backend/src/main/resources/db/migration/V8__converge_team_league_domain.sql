-- Convergencia al esquema definitivo de SPEC-007 (Player -> Team -> League) para bases que
-- aplicaron la forma aditiva de V5 antes de que ese archivo fuera reescrito.
--
-- V5 se aplicó en dos formas sucesivas en el historial del repositorio. La base de desarrollo
-- conserva la forma aditiva: player.team y player.league presentes, player.team_id nullable y
-- una tabla catalog_transition de control manual. Esta migración lleva esa base al mismo estado
-- que obtiene una instalación limpia con el V5 actual, sin recrear la base y sin perder jugadores.
--
-- Es idempotente: si player ya no expone los textos legacy, el bloque de datos no hace nada y solo
-- se completan la restricción de unicidad del SUPERUSER y la ausencia de la tabla de control.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'player'
          AND column_name = 'league'
    ) THEN
        -- Cada liga distinta del catálogo legacy se materializa una sola vez.
        INSERT INTO leagues(name)
        SELECT DISTINCT btrim(p.league)
        FROM players p
        WHERE p.league IS NOT NULL
          AND btrim(p.league) <> ''
          AND NOT EXISTS (SELECT 1 FROM leagues l WHERE l.name = btrim(p.league));

        -- La liga de un equipo se deriva del texto legacy del jugador; la resolución de identidad
        -- del dominio es por nombre normalizado, de modo que el próximo catálogo los reutiliza en
        -- lugar de duplicarlos.
        INSERT INTO teams(name, league_id, current)
        SELECT DISTINCT btrim(p.team), l.id, true
        FROM players p
        JOIN leagues l ON l.name = btrim(p.league)
        WHERE p.team IS NOT NULL
          AND btrim(p.team) <> ''
          AND NOT EXISTS (
              SELECT 1 FROM teams t WHERE t.name = btrim(p.team) AND t.league_id = l.id
          );

        UPDATE players p
        SET team_id = t.id
        FROM teams t
        JOIN leagues l ON l.id = t.league_id
        WHERE t.name = btrim(p.team)
          AND l.name = btrim(p.league);

        -- Todo Player persistido tiene exactamente un Team. Si queda alguna fila sin asociación no
        -- se descarta: la migración falla y el catálogo puede regenerarse desde la fuente externa.
        IF EXISTS (SELECT 1 FROM players WHERE team_id IS NULL) THEN
            RAISE EXCEPTION
                'No se puede volver player.team_id obligatorio: quedan % jugadores sin equipo derivado',
                (SELECT count(*) FROM players WHERE team_id IS NULL);
        END IF;

        ALTER TABLE players DROP COLUMN IF EXISTS "team";
        ALTER TABLE players DROP COLUMN IF EXISTS "league";
        ALTER TABLE players ALTER COLUMN team_id SET NOT NULL;
    END IF;
END
$$;

-- La tabla de control manual solo tenía sentido en la forma aditiva de V5.
DROP TABLE IF EXISTS catalog_transition;

-- La categoría LEGACY_TEAM_ASSOCIATION solo era admisible en la forma aditiva de V5.
ALTER TABLE pending_review_cases DROP CONSTRAINT IF EXISTS ck_pending_review_cases_category;
ALTER TABLE pending_review_cases
    ADD CONSTRAINT ck_pending_review_cases_category CHECK (
        category IN ('PLAYER_TEAM_UNRESOLVED', 'TEAM_LEAGUE_UNRESOLVED',
                     'EXTERNAL_IDENTITY_CONFLICT', 'PLAYER_OPTIONAL_CONFLICT',
                     'INVALID_SUBJECT_DATA'));