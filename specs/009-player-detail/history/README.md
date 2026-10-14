# Convenciones del historial de interacciones

Carpeta generada y mantenida por `tools/evidence-capture.py` (piloto).

## Estructura

- `index.md`: índice de intercambios. Documental; no altera estados SDD.
- `prompts/<session_id>/<NNNN>.md`: un archivo por intercambio.

## Unidad de registro

Un intercambio comienza con un prompt del usuario y termina justo antes del
siguiente prompt del usuario. Contiene el prompt literal y **todas** las
respuestas visibles del agente asociadas, en su orden original.

No se guardan partes de razonamiento (traza interna) ni se presentan salidas de
herramientas como si fueran respuestas visibles: las herramientas se resumen en
la sección de evidencias.

## Identidad canónica

`interaction_id` (en el front matter) es estable y se deriva del identificador
real del mensaje del usuario. Reejecutar la captura no crea duplicados ni
sobrescribe el original.

## Reglas de seguridad

El contenido con indicios de material sensible no se escribe en el repositorio:
se deposita en el almacén protegido (`FOOTBALL_MARKET_PROTECTED_STORE` o
`~/.local/share/football-market/history-protected/`) y requiere revisión humana.
La detección combina patrones estructurales, pares clave/valor, análisis de
entropía y allowlist de identificadores; ante duda se bloquea la escritura.

## Límites

- No se trunca contenido de forma silenciosa. Lo que supera el umbral se
  referencia con ruta y SHA-256, y queda en el almacén protegido.
- `capture_status` refleja completitud de la captura; no es un veredicto técnico.
- `gate_human` siempre es `NO EVALUADO`: este sistema no aprueba gates.
