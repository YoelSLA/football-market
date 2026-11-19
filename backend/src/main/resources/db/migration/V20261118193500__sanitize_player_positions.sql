-- Corrige la posición deportiva de Rubén Sánchez (Elche CF) a su valor real verificado (Defence)
UPDATE players
SET position = 'Defence'
WHERE id = 1468 AND lower(trim(position)) = 'null';

-- Desactiva preventivamente cualquier otro registro con posición inválida residual
UPDATE players
SET active = false
WHERE lower(trim(position)) = 'null' OR trim(position) = '';

-- Restringe la persistencia de posiciones nulas en texto o en blanco
ALTER TABLE players
ADD CONSTRAINT ck_players_position_valid
CHECK (trim(position) <> '' AND lower(trim(position)) <> 'null');
