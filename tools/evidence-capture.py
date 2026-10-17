#!/usr/bin/env python3
"""Piloto de captura de historial de interacciones OpenCode -> Markdown.

Fuente de verdad: base SQLite local de OpenCode (LECTURA ESTRICTAMENTE READ-ONLY).
No escribe en la base, no invoca la CLI de OpenCode, no toca Trello ni el
workflow SDD, no modifica tareas, gates ni estados.

Uso (solo lectura sobre la base; escritura limitada a las rutas del piloto):

  python3 tools/evidence-capture.py list
  python3 tools/evidence-capture.py capture --session ses_xxx [--max-exchanges N]
  python3 tools/evidence-capture.py capture --last N --max-exchanges N
  python3 tools/evidence-capture.py index
  python3 tools/evidence-capture.py verify --session ses_xxx
  python3 tools/evidence-capture.py report

Alcance de escritura de este piloto:
  specs/009-player-detail/history/**, specs/009-player-detail/evidence/**
  evidencias/**
Cualquier otra SPEC (incluida SPEC-005 y SPEC-008) queda fuera.
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import re
import sqlite3
import sys
from pathlib import Path

# --------------------------------------------------------------------------
# Constantes de configuracion del piloto
# --------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parent.parent
SPECS_DIR = REPO_ROOT / "specs"

PILOT_SPEC_NUM = 9                      # unica SPEC con escritura permitida
PILOT_SPEC_DIRNAME = "009-player-detail"
GLOBAL_DIRNAME = "evidencias"
DENIED_SPEC_NUMBERS = {5, 8}             # 005 expansion posterior; 008 excluida

# Umbrales (bytes). No se trunca nada silenciosamente: por encima del umbral el
# Markdown referencia el contenido completo depositado fuera del repositorio.
MAX_INLINE_BYTES = 200_000               # cuerpo del intercambio en el .md
MAX_EXCERPT_CHARS = 240                  # extracto de salida de herramienta
FULL_COPY_DIRNAME = "history-full"       # dentro de evidencias/ (fuera de SPEC)

VISIBLE_PART_TYPES = {"text"}            # unicamente lo visible para el usuario
EXCLUDED_PART_TYPES = {"reasoning"}      # traza interna: no se publica

STAGE_KEYWORDS = [
    ("SPECIFY", r"\bspecify\b|especificar|crear la spec"),
    ("CLARIFY", r"\bclarify\b|aclarar"),
    ("PLAN", r"\bplan\b|planific"),
    ("CHECKLISTS", r"checklist"),
    ("TASKS", r"\btasks?\b|tareas"),
    ("ANALYZE", r"\banalyze\b|análisis|analisis"),
    ("IMPLEMENT", r"\bimplement\b|implementar|implementación"),
    ("CONVERGE", r"converge"),
    ("CODE REVIEW", r"code review|revisión de código"),
    ("TESTING", r"\btesting\b|pruebas"),
]

TOOL_WRITE_NAMES = {"edit", "write", "patch", "multiedit", "apply_patch"}

# Aliases curados y minimos por numero de SPEC. Se evitan coincidencias por
# palabras sueltas (p.ej. "page", "user", "data") que generaban falsos positivos.
SPEC_ALIASES = {
    9: ("009-player-detail", "player detail", "playerdetail", "detalle de jugador",
        "detalle del jugador"),
    5: ("005-players-page", "players page", "players-page", "pagina de jugadores",
        "página de jugadores"),
}


# --------------------------------------------------------------------------
# Rutas y base de datos
# --------------------------------------------------------------------------

def find_db() -> Path:
    """Localiza la base de sesiones de OpenCode sin escribir nada."""
    env = os.environ.get("OPENCODE_DB")
    candidates = [Path(env)] if env else []
    xdg = os.environ.get("XDG_DATA_HOME")
    if xdg:
        candidates.append(Path(xdg) / "opencode" / "opencode.db")
    home = Path.home()
    candidates += [
        home / ".local" / "share" / "opencode" / "opencode.db",
        home / "Library" / "Application Support" / "opencode" / "opencode.db",
        home / ".opencode" / "opencode.db",
    ]
    for c in candidates:
        if c.is_file():
            return c
    raise SystemExit(
        "ERROR: no se localizó opencode.db. Definí OPENCODE_DB con la ruta exacta."
    )


def open_readonly(db: Path) -> sqlite3.Connection:
    uri = f"file:{db}?mode=ro"
    return sqlite3.connect(uri, uri=True)


# --------------------------------------------------------------------------
# Utilidades
# --------------------------------------------------------------------------

def iso(ms):
    if ms is None:
        return None
    return dt.datetime.fromtimestamp(ms / 1000, dt.timezone.utc).strftime(
        "%Y-%m-%dT%H:%M:%SZ"
    )


def spec_id(num: int) -> str:
    return f"SPEC-{num:03d}"


def spec_dirname(num: int):
    if not SPECS_DIR.is_dir():
        return None
    prefix = f"{num:03d}-"
    for d in sorted(SPECS_DIR.iterdir()):
        if d.is_dir() and d.name.startswith(prefix):
            return d
    return None


def sha256_bytes(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()


def rel(p) -> str:
    p = Path(p)
    try:
        return str(p.relative_to(REPO_ROOT)).replace(os.sep, "/")
    except ValueError:
        return str(p)  # fuera del repositorio (almacen protegido)


# --------------------------------------------------------------------------
# Deteccion de material sensible (no se apoya solo en una unica regex)
#
# Capas: (1) patrones estructurales de secretos conocidos; (2) pares
# clave/valor con nombre sensibile; (3) entropia; (4) allowlist explicita de
# identificadores y hashes propios del sistema; (5) politica conservadora:
# ante duda se BLOQUEA la escritura en ruta versionable y se escala a revision
# humana. Nunca se afirma que un archivo esta "saneado".
# --------------------------------------------------------------------------

PLACEHOLDER_RE = re.compile(
    r"^(?:<[^>]*>|\$\{?[A-Z0-9_]+\}?|%[A-Z0-9_]+%|\{\{.*\}\}|x{3,}|\*{3,}"
    r"|REDACTED|TODO|NONE|NULL|EXAMPLE|SAMPLE|CHANGEME|DUMMY|PLACEHOLDER"
    r"|[A-Z0-9_]{0,40}(?:KEY|TOKEN|SECRET|PASSWORD)[A-Z0-9_]{0,40})$",
    re.IGNORECASE,
)
SECRET_NAME_RE = re.compile(
    r"(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|auth[_-]?token|bearer|"
    r"client[_-]?secret|secret[_-]?key|password|passwd|pwd|secret|token)"
)
SECRET_KV_RE = re.compile(
    r"(?i)[\"']?(?P<k>[A-Za-z0-9_.\-]*(?:password|passwd|pwd|secret|token|api[_-]?key)"
    r"|authorization)[\"']?\s*[:=]\s*[\"']?(?P<v>[^\s\"',}\]]{8,})"
)
STRUCTURAL_RULES = [
    ("pem-private-key", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")),
    ("jwt", re.compile(r"\beyJ[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{5,}")),
    ("provider-key", re.compile(
        r"\b(?:sk-[A-Za-z0-9]{16,}|sk-ant-[A-Za-z0-9\-_]{16,}|ghp_[A-Za-z0-9]{20,}"
        r"|gho_[A-Za-z0-9]{20,}|xox[baprs]-[A-Za-z0-9\-]{10,}|AKIA[0-9A-Z]{16})")),
    ("basic-auth-url", re.compile(r"\b[a-z][a-z0-9+.\-]*://[^\s/:@]+:[^\s/@]{6,}@")),
]
ENTROPY_RE = re.compile(r"\b(?P<v>[A-Za-z0-9+/_\-]{40,}={0,2})\b")
CREDENTIAL_PATH_RE = re.compile(
    r"(?i)(?:^|[\s\"'(])((?:[\w./~-]+/)?(?:\.env(?:\.\w+)?|trello\.env|service\.json|"
    r"credentials(?:\.\w+)*|id_rsa|id_ed25519|\.netrc|\.npmrc))$"
)
# Allowlist: identificadores y hashes propios de OpenCode/este sistema.
ALLOW_RE = re.compile(
    r"^(?:ses_[A-Za-z0-9]+|msg_[A-Za-z0-9]+|call_[A-Za-z0-9_]+|[a-f0-9]{32}|[a-f0-9]{40}"
    r"|[a-f0-9]{64}|[A-Za-z0-9_-]{8,12})$"
)


PATH_HINT_RE = re.compile(
    r"(?:^|/)(?:specs|src|docs|frontend|backend|tools|evidencias|node_modules|"
    r"components|features|shared|app|styles|main|test|resources|public|\.github|"
    r"\.opencode|\.specify)(?:/|$)"
)
FILE_EXT_RE = re.compile(r"\.[A-Za-z]{1,6}$")


def looks_like_path(value: str) -> bool:
    """Las rutas de archivos del repo tienen alta entropia y no son secretos.

    Refina la deteccion por entropia. Riesgo residual aceptado: un secreto que
    contenga '/' y un nombre de directorio conocido no se detectaria aqui; la
    politica sigue siendo bloqueante ante cualquier otro indicio.
    """
    if "/" not in value:
        return False
    if PATH_HINT_RE.search(value):
        return True
    return bool(FILE_EXT_RE.search(value))


def _entropy(value: str) -> float:
    if not value:
        return 0.0
    from collections import Counter
    import math
    counts = Counter(value)
    n = len(value)
    return -sum((c / n) * math.log2(c / n) for c in counts.values())


def scan_sensitive(text: str):
    """Devuelve lista de hallazgos: (regla, severidad, extracto enmascarado)."""
    findings = []
    if not text:
        return findings
    for rule, rx in STRUCTURAL_RULES:
        for m in rx.finditer(text):
            findings.append((rule, "BLOCK", _mask(m.group(0))))
    for m in SECRET_KV_RE.finditer(text):
        value = m.group("v")
        if PLACEHOLDER_RE.match(value) or ALLOW_RE.match(value):
            continue
        findings.append(("clave-valor-sensible", "BLOCK", f"{m.group('k')}=<valor oculto>"))
    for m in ENTROPY_RE.finditer(text):
        value = m.group("v")
        if ALLOW_RE.match(value) or looks_like_path(value):
            continue
        if _entropy(value) >= 4.0 and re.search(r"[A-Za-z]", value) and re.search(r"\d", value):
            findings.append(("cadena-alta-entropia", "BLOCK", _mask(value)))
    for m in CREDENTIAL_PATH_RE.finditer(text):
        findings.append(("referencia-a-archivo-de-credenciales", "WARN", m.group(1)))
    return findings


def _mask(value: str) -> str:
    if len(value) <= 8:
        return "*" * len(value)
    return f"{value[:4]}…{value[-4:]} ({len(value)} chars)"


def worst_severity(findings):
    if any(f[1] == "BLOCK" for f in findings):
        return "BLOCK"
    if any(f[1] == "WARN" for f in findings):
        return "WARN"
    return "OK"


# --------------------------------------------------------------------------
# Lectura de sesiones y construccion de intercambios
# --------------------------------------------------------------------------

def load_sessions(conn, directory=None):
    q = ("SELECT id, slug, directory, title, parent_id, agent, model, version,"
         " time_created, time_updated FROM session_v2")
    args = []
    if directory:
        q += " WHERE directory = ?"
        args.append(directory)
    q += " ORDER BY time_created DESC"
    return [dict(zip(
        ["id", "slug", "directory", "title", "parent_id", "agent", "model",
         "version", "time_created", "time_updated"], row)) for row in conn.execute(q, args)]


def load_messages(conn, session_id):
    rows = conn.execute(
        "SELECT id, seq, type, time_created, time_updated, data FROM session_message"
        " WHERE session_id = ? ORDER BY seq", (session_id,)).fetchall()
    out = []
    for mid, seq, mtype, tc, tu, data in rows:
        try:
            payload = json.loads(data)
        except json.JSONDecodeError:
            payload = {"__unparsable__": True}
        out.append({"id": mid, "seq": seq, "type": mtype,
                    "time_created": tc, "time_updated": tu, "data": payload})
    return out


def build_exchanges(messages):
    """Cada prompt del usuario abre un intercambio; se cierra antes del
    siguiente prompt del usuario. Solo se conserva lo visible (partes text)."""
    exchanges = []
    current = None
    for m in messages:
        if m["type"] == "user":
            current = {
                "user_message_id": m["id"],
                "user_seq": m["seq"],
                "user_time": m["time_created"],
                "prompt_text": m["data"].get("text") or "",
                "prompt_files": m["data"].get("files") or [],
                "prompt_agents": m["data"].get("agents") or [],
                "assistant_messages": [],
                "parts_summary": {"text": 0, "reasoning": 0, "tool": 0, "other": 0},
            }
            exchanges.append(current)
        elif m["type"] == "assistant" and current is not None:
            visible, tools, reasoning = [], [], 0
            finish = None
            for part in (m["data"].get("content") or []):
                ptype = part.get("type")
                if ptype in VISIBLE_PART_TYPES:
                    txt = part.get("text") or ""
                    visible.append(txt)
                    current["parts_summary"]["text"] += 1
                elif ptype == "reasoning":
                    reasoning += 1
                    current["parts_summary"]["reasoning"] += 1
                elif ptype == "tool":
                    tools.append(part)
                    current["parts_summary"]["tool"] += 1
                else:
                    current["parts_summary"]["other"] += 1
            finish = m["data"].get("finish")
            current["assistant_messages"].append({
                "message_id": m["id"],
                "seq": m["seq"],
                "time_created": m["time_created"],
                "agent": m["data"].get("agent"),
                "model": m["data"].get("model"),
                "finish": finish,
                "visible_parts": visible,
                "tools": tools,
                "reasoning_parts": reasoning,
            })
    for ex in exchanges:
        ex["interaction_id"] = "ix_" + sha256_bytes(
            f"{ex['user_message_id']}".encode())[:16]
    return exchanges


def classify(exchange, session):
    """Clasificacion por SPEC usando contexto, no solo la rama Git."""
    haystack = " \n".join([
        session.get("title") or "",
        exchange["prompt_text"][:20000],
        " ".join(" ".join(m["visible_parts"]) for m in exchange["assistant_messages"])[:20000],
        " ".join(_tool_haystack(m["tools"]) for m in exchange["assistant_messages"]),
    ])

    low0 = haystack.lower()
    nums = set()
    for m in re.finditer(r"(?i)\bspec[-_ ]?0*(\d{1,3})\b", haystack):
        nums.add(int(m.group(1)))
    low = haystack.lower()
    # Coincidencia por nombre de carpeta de spec, verbatim (p.ej. "009-player-detail").
    for d in sorted(SPECS_DIR.glob("*-*")) if SPECS_DIR.is_dir() else []:
        num = d.name.split("-", 1)[0]
        if num.isdigit() and d.name.lower() in low:
            nums.add(int(num))
    # Alias curados y explicitos por SPEC (evitan falsos positivos por palabras sueltas).
    for num, aliases in SPEC_ALIASES.items():
        if any(a in low for a in aliases):
            nums.add(num)

    tasks = sorted(set(re.findall(r"\bT\d{3}\b", haystack)))
    stage = "UNSPECIFIED"
    for name, pattern in STAGE_KEYWORDS:
        if re.search(pattern, haystack, re.IGNORECASE):
            stage = name
            break

    allowed = sorted(n for n in nums if n not in DENIED_SPEC_NUMBERS)
    denied = sorted(n for n in nums if n in DENIED_SPEC_NUMBERS)
    status, reason = "OK", ""
    if not allowed:
        canonical = None
        if not nums:
            status = "GLOBAL"
            reason = "No se identifica ninguna SPEC: historial global"
        else:
            status = "PENDING_CLASSIFICATION"
            reason = ("SPEC mencionada(s) fuera del alcance del piloto ("
                      + ", ".join(spec_id(n) for n in denied) + "): requiere "
                      "confirmacion humana antes de asignar ubicacion canonica")
    elif allowed == [PILOT_SPEC_NUM]:
        canonical, status = PILOT_SPEC_NUM, "OK"
        reason = f"Coincidencia única con {spec_id(PILOT_SPEC_NUM)}"
    elif PILOT_SPEC_NUM in allowed:
        canonical = PILOT_SPEC_NUM
        status, reason = "OK", (
            f"{spec_id(PILOT_SPEC_NUM)} canónica; SPEC secundarias: " +
            ", ".join(spec_id(n) for n in allowed if n != PILOT_SPEC_NUM)
        )
    else:
        canonical = None
        status, reason = "PENDING_CLASSIFICATION", (
            "SPEC detectadas fuera del piloto: " +
            ", ".join(spec_id(n) for n in allowed)
        )
    return {
        "canonical": canonical,
        "related": [n for n in allowed if n != canonical],
        "denied": denied,
        "status": status,
        "reason": reason,
        "tasks": tasks,
        "stage": stage,
        "detected": sorted(nums),
    }


def _tool_haystack(tools):
    out = []
    for t in tools or []:
        st = t.get("state") or {}
        inp = st.get("input") or {}
        for key in ("path", "filePath", "command", "pattern", "description", "query"):
            v = inp.get(key)
            if isinstance(v, str):
                out.append(v[:400])
    return " ".join(out)


def tool_rows(exchange):
    """Resumen de herramientas: no se interpreta 'completed' como PASS."""
    rows = []
    for m in exchange["assistant_messages"]:
        for t in m["tools"]:
            st = t.get("state") or {}
            inp = st.get("input") or {}
            cmd = inp.get("command") or inp.get("path") or inp.get("filePath") or ""
            excerpt = _excerpt(st.get("content"))
            rows.append({
                "message_id": m["message_id"],
                "tool": t.get("name"),
                "executed": t.get("executed"),
                "status": st.get("status"),
                "input": str(cmd)[:300],
                "exit_code": _exit_code(st.get("content")),
                "output_excerpt": excerpt,
            })
    return rows


def _excerpt(content):
    if not content:
        return None
    text = " ".join(
        c.get("text", "") for c in content if isinstance(c, dict)) if isinstance(content, list) \
        else str(content)
    text = " ".join(text.split())
    if not text:
        return None
    return text[:MAX_EXCERPT_CHARS] + ("…" if len(text) > MAX_EXCERPT_CHARS else "")


def _exit_code(content):
    ex = _excerpt(content)
    if not ex:
        return None
    m = re.search(r"EXIT\s*[=:]\s*(\d+)", ex, re.IGNORECASE)
    return int(m.group(1)) if m else None


def modified_files(exchange):
    files = []
    for m in exchange["assistant_messages"]:
        for t in m["tools"]:
            if t.get("name") in TOOL_WRITE_NAMES:
                st = t.get("state") or {}
                inp = st.get("input") or {}
                p = inp.get("path") or inp.get("filePath")
                if p:
                    files.append(p)
    seen, ordered = set(), []
    for f in files:
        if f not in seen:
            seen.add(f)
            ordered.append(f)
    return ordered


def completion_state(exchange, session_is_last):
    if not exchange["assistant_messages"]:
        return "INTERRUPTED", "Sin mensaje de asistente asociado al prompt"
    last = exchange["assistant_messages"][-1]
    statuses = [(t.get("state") or {}).get("status") for m in exchange["assistant_messages"]
                for t in m["tools"]]
    visible = sum(len(m["visible_parts"]) for m in exchange["assistant_messages"])
    if last.get("finish") is None and not statuses:
        return "INTERRUPTED", "El último mensaje visible no registra fin de respuesta"
    if any(s == "running" for s in statuses) and session_is_last:
        return "IN_PROGRESS", "Hay herramientas en estado running en la sesión actual"
    if any(s == "error" for s in statuses):
        return "PARTIAL", "Alguna herramienta registró estado error"
    if visible == 0:
        return "PARTIAL", "No se identificó contenido visible del asistente"
    return "COMPLETE", "Prompt y respuesta(s) visible(s) preservados"


# --------------------------------------------------------------------------
# Rutas de salida
# --------------------------------------------------------------------------

def scope_dir(canonical):
    if canonical == PILOT_SPEC_NUM:
        return SPECS_DIR / PILOT_SPEC_DIRNAME
    return REPO_ROOT / GLOBAL_DIRNAME


def write_scope(canonical, *parts):
    return scope_dir(canonical).joinpath(*parts)


def existing_records(scope: Path):
    """Registros ya presentes: se usa el front matter como registro de dedupe."""
    out = {}
    prompts = scope / "history" / "prompts"
    if prompts.is_dir():
        for f in sorted(prompts.glob("*/[0-9][0-9][0-9][0-9].md")):
            fm = read_frontmatter(f)
            if fm.get("interaction_id"):
                out[fm["interaction_id"]] = f
    return out


def read_frontmatter(path: Path):
    text = path.read_text(encoding="utf-8")
    if not text.startswith("---"):
        return {}
    end = text.find("\n---", 3)
    if end == -1:
        return {}
    block = text[3:end]
    data, key = {}, None
    for line in block.splitlines():
        m = re.match(r"^([A-Za-z_][A-Za-z0-9_]*):\s*(.*)$", line)
        if m:
            key = m.group(1)
            val = m.group(2).strip()
            data[key] = [] if val.startswith("[") and val.endswith("]") and not val[1:-1] else (
                val.strip("\"'") if val not in ("[]",) else [])
        elif line.strip().startswith("- ") and key:
            data.setdefault(key, [])
            if isinstance(data[key], list):
                data[key].append(line.strip()[2:].strip("\"'"))
    if "assistant_message_ids" in data and isinstance(data["assistant_message_ids"], list):
        data["assistant_message_ids"] = ",".join(data["assistant_message_ids"])
    return data


def next_sequence(session_id, scope: Path, reserved):
    d = scope / "history" / "prompts" / session_id
    used = set(reserved)
    if d.is_dir():
        for f in d.glob("*.md"):
            m = re.match(r"^(\d{4})\.md$", f.name)
            if m:
                used.add(int(m.group(1)))
    n = 1
    while n in used:
        n += 1
    return n


# --------------------------------------------------------------------------
# Renderizado del registro
# --------------------------------------------------------------------------

def yaml_scalar(v):
    if v is None:
        return "null"
    s = str(v)
    if s == "" or re.search(r"[:#\[\]{}]|^\s|\s$", s):
        return '"' + s.replace('"', '\\"') + '"'
    return s


def render_record(exchange, session, cls, state, tool_summary, full_path, full_sha, warnings):
    a_ids = [m["message_id"] for m in exchange["assistant_messages"]]
    lines = []
    add = lines.append

    # ---- front matter (identidad canonica) ----
    add("---")
    add(f"interaction_id: {exchange['interaction_id']}")
    add(f"session_id: {session['id']}")
    add(f"session_slug: {yaml_scalar(session.get('slug'))}")
    add(f"session_title: {yaml_scalar(session.get('title'))}")
    add(f"user_message_id: {exchange['user_message_id']}")
    add("assistant_message_ids: [" + ", ".join(a_ids) + "]")
    add(f"session_role: {'SUBAGENT' if session.get('parent_id') else 'PRINCIPAL'}")
    add(f"agent: {yaml_scalar(exchange['assistant_messages'][0]['agent'] if exchange['assistant_messages'] else None)}")
    add(f"spec: {spec_id(cls['canonical']) if cls['canonical'] else 'null'}")
    add(f"related_specs: [{', '.join(spec_id(n) for n in cls['related'])}]")
    add("excluded_specs: [" + ", ".join(spec_id(n) for n in cls["denied"]) + "]")
    add(f"tasks: [{', '.join(cls['tasks'])}]")
    add(f"stage: {cls['stage']}")
    add(f"classification: {cls['status']}")
    add(f"created_at: {iso(exchange['user_time'])}")
    add(f"updated_at: {iso(exchange['user_time'])}")
    add(f"capture_status: {state[0]}")
    add("capture_method: AUTOMATIC_READ_ONLY_SQLITE")
    add("provenance: opencode.session_message (mode=ro)")
    add(f"detail: {yaml_scalar(state[1])}")
    add("gate_human: NO EVALUADO")
    add("---")

    add("")
    add(f"# Intercambio {exchange.get('seq_label', '')}".rstrip())
    add("")
    add("## Información")
    add("")
    add(f"- **Fecha (UTC)**: {iso(exchange['user_time'])}")
    add(f"- **Sesión**: `{session['id']}` ({session.get('slug')})")
    add(f"- **Título de sesión**: {session.get('title') or '—'}")
    add(f"- **Rol de la sesión**: {'subagente' if session.get('parent_id') else 'principal'}")
    add(f"- **Agente**: {exchange['assistant_messages'][0]['agent'] if exchange['assistant_messages'] else 'no disponible'}")
    add(f"- **Modelo**: {exchange['assistant_messages'][0]['model'] if exchange['assistant_messages'] else 'no disponible'}")
    add(f"- **SPEC canónica**: {spec_id(cls['canonical']) if cls['canonical'] else '— (historial global)'}")
    add(f"- **SPEC relacionadas**: {', '.join(spec_id(n) for n in cls['related']) or '—'}")
    add(f"- **SPEC excluidas del piloto**: {', '.join(spec_id(n) for n in cls['denied']) or '—'}")
    add(f"- **Tareas SDD citadas**: {', '.join(cls['tasks']) or '—'}")
    add(f"- **Etapa SDD**: {cls['stage']} (metadato informativo; el sistema no la modifica)")
    add(f"- **Clasificación**: {cls['status']} — {cls['reason']}")
    add(f"- **Gate humano**: NO EVALUADO (este registro no aprueba ni altera ningún gate)")
    add("")

    add("## Prompt original del usuario")
    add("")
    add(f"_Origen: `session_message` tipo `user`, id `{exchange['user_message_id']}`, "
        f"seq `{exchange['user_seq']}`, `{iso(exchange['user_time'])}`. Texto literal, sin resumen._")
    add("")
    add("```text")
    add(exchange["prompt_text"])
    add("```")
    if exchange["prompt_files"]:
        add("")
        add(f"_Ficheros adjuntos en el prompt: {len(exchange['prompt_files'])}._")
    add("")

    add("## Respuestas visibles originales del agente")
    add("")
    add("_Contenido de tipo `text` de los mensajes `assistant` asociados, en su orden "
        "original (`seq`). Las partes `reasoning` no se incluyen por ser traza interna "
        "no destinada al usuario._")
    add("")
    if not exchange["assistant_messages"]:
        add("_No se recuperó ningún mensaje de asistente asociado._")
        add("")
        add("> **PENDIENTE DE VALIDACIÓN**: no es posible determinar con certeza si la "
            "respuesta existió y no fue persistida.")
        add("")
    else:
        n = 0
        for m in exchange["assistant_messages"]:
            for part_text in m["visible_parts"]:
                n += 1
                add(f"### Respuesta {n}")
                add("")
                add(f"_`{m['message_id']}` · seq `{m['seq']}` · {iso(m['time_created'])}"
                    + (f" · finish=`{m['finish']}`" if m.get("finish") else "") + "_")
                add("")
                add("```text")
                add(part_text)
                add("```")
                add("")
        if n == 0:
            add("_Los mensajes de asistente no contienen partes de tipo `text` "
                "identificables como visibles._")
            add("")
            add("> **PENDIENTE DE VALIDACIÓN**: el contenido visible no pudo "
                "determinarse con certeza a partir de los datos persistidos.")
            add("")

    if full_path:
        add("## Contenido extenso (almacenamiento protegido)")
        add("")
        add(f"El cuerpo literal completo de este intercambio supera "
            f"{MAX_INLINE_BYTES} bytes y **no se incorporated automáticamente al repositorio**.")
        add("")
        add(f"- Ubicación: `{full_path}`")
        add(f"- SHA-256: `{full_sha}`")
        add("- Copia canónica: única (no se duplica en otras SPEC).")
        add("- Requiere decisión humana sobre su incorporación o exclusión de Git.")
        add("")

    add("## Evidencias relacionadas")
    add("")
    add("_Sección separada del texto de conversación. Ninguna fila implica PASS ni "
        "aprobación de gate: `completed` describe el estado de la herramienta, no el "
        "resultado técnico ni el gate humano._")
    add("")
    if not tool_summary:
        add("_No hay partes de tipo `tool` registradas para este intercambio._")
        add("")
    else:
        add("| # | Herramienta | Mensaje | Estado de herramienta | Ejecutada | Entrada (recortada) | Código de salida | Extracto de salida |")
        add("| --- | --- | --- | --- | --- | --- | --- | --- |")
        for i, r in enumerate(tool_summary, 1):
            add(f"| {i} | `{r['tool']}` | `{r['message_id']}` | {r['status'] or 'UNKNOWN'} "
                f"| {r['executed'] if r['executed'] is not None else 'UNKNOWN'} "
                f"| `{_one_line(r['input'])}` "
                f"| {r['exit_code'] if r['exit_code'] is not None else 'UNKNOWN'} "
                f"| {_one_line(r['output_excerpt']) or '—'} |")
        add("")
        mods = modified_files(exchange)
        add("**Archivos referenciados por herramientas de escritura "
            "(`edit`/`write`/`patch`), no verificados contra el sistema de archivos por este registro:**")
        add("")
        if mods:
            add("\n".join(f"- `{m}`" for m in mods))
        else:
            add("- —")
        add("")

    add("## Estado de captura")
    add("")
    add(f"- **{state[0]}** — {state[1]}")
    add(f"- Partes visibles: {exchange['parts_summary']['text']} · "
        f"herramientas: {exchange['parts_summary']['tool']} · "
        f"razonamiento excluido: {exchange['parts_summary']['reasoning']}")
    add("- Regenerar este registro no debe producir duplicados: la identidad canónica es "
        "`interaction_id`.")
    add("")

    add("## Procedencia")
    add("")
    add("- Base: base de datos local de OpenCode `opencode.db`, abierto en modo "
        "**solo lectura** (`mode=ro`).")
    add(f"- Sesión: `{session['id']}` · Prompt: `{exchange['user_message_id']}` · "
        f"Mensajes de asistente: {', '.join('`'+a+'`' for a in a_ids) or '—'}")
    add("- No se ejecutó importación de sesiones ni se escribió en la base.")
    add("- No se modificaron `tasks.md`, `specs/trello.md`, Trello, la Constitución ni "
        "el workflow SDD.")
    add("")

    add("## Advertencias de seguridad")
    add("")
    if warnings:
        for w in warnings:
            add(f"- **{w['severity']}** `{w['rule']}`: {w['detail']}")
    else:
        add("- Sin hallazgos de material sensible en el contenido capturado.")
        add("- Esto no constituye una garantía de ausencia de secretos en el resto del "
            "entorno: el contenido sensible identificado en la base de OpenCode "
            "(cuentas y credenciales) no se lee ni se copia.")
    add("")
    return "\n".join(lines) + "\n"


def _one_line(text, limit=120):
    if not text:
        return "—"
    t = " ".join(str(text).split())
    return t[:limit] + ("…" if len(t) > limit else "")


# --------------------------------------------------------------------------
# Captura
# --------------------------------------------------------------------------

def capture(conn, sessions, max_exchanges, out=sys.stdout):
    report = {"written": [], "skipped": [], "blocked": [], "pending": []}
    last_session = sessions[0]["id"] if sessions else None
    for session in sessions:
        messages = load_messages(conn, session["id"])
        exchanges = build_exchanges(messages)
        if max_exchanges and len(exchanges) > max_exchanges:
            exchanges = exchanges[-max_exchanges:]
        for ex in exchanges:
            cls = classify(ex, session)
            scope = scope_dir(cls["canonical"])
            existing = existing_records(scope)
            if ex["interaction_id"] in existing:
                report["skipped"].append({
                    "interaction_id": ex["interaction_id"],
                    "path": rel(existing[ex["interaction_id"]]),
                    "reason": "registro ya existente; no se duplica"})
                continue
            state = completion_state(ex, session["id"] == last_session)

            # ---- contenido a evaluar ----
            body_parts = [ex["prompt_text"]]
            for m in ex["assistant_messages"]:
                body_parts.extend(m["visible_parts"])
            body_parts.extend(_tool_haystack(sum([m["tools"] for m in ex["assistant_messages"]], [])))
            scan_text = "\n".join(p for p in body_parts if p)
            findings = scan_sensitive(scan_text)
            severity = worst_severity(findings)
            masked = [(f[0], f[1], f[2]) for f in findings]
            warn_list = [{"rule": r, "severity": s, "detail": d} for r, s, d in masked
                         if s == "WARN"]

            if severity == "BLOCK":
                blocked = register_blocked(ex, session, cls, masked)
                report["blocked"].append(blocked)
                print(f"[BLOQUEADO] {ex['interaction_id']} — {rel(Path(blocked['store']))} "
                      f"(requiere revisión humana)", file=out)
                continue

            n = next_sequence(session["id"], scope, set())
            ex["seq_label"] = f"{n:04d}"
            full_rel = full_sha = None
            body_bytes = "\n".join(body_parts).encode("utf-8")
            record = render_record(ex, session, cls, state,
                                   tool_rows(ex), None, None, warn_list)
            if len(body_bytes) > MAX_INLINE_BYTES:
                full_rel, full_sha = deposit_full_copy(ex, body_parts)
                record = render_record(ex, session, cls, state,
                                       tool_rows(ex), full_rel, full_sha, warn_list)

            path = scope / "history" / "prompts" / session["id"] / f"{n:04d}.md"
            path.parent.mkdir(parents=True, exist_ok=True)
            if path.exists():
                report["skipped"].append({"interaction_id": ex["interaction_id"],
                                         "path": rel(path), "reason": "archivo existente"})
                continue
            path.write_text(record, encoding="utf-8")
            entry = {"interaction_id": ex["interaction_id"],
                     "session": session["id"], "file": rel(path),
                     "spec": spec_id(cls["canonical"]) if cls["canonical"] else "global",
                     "status": state[0], "classification": cls["status"],
                     "tasks": cls["tasks"], "stage": cls["stage"],
                     "created_at": iso(ex["user_time"]),
                     "bytes": path.stat().st_size}
            report["written"].append(entry)
            print(f"[OK] {entry['spec']:9s} {rel(path)}", file=out)
            if cls["status"] != "OK":
                report["pending"].append(entry)

    for scope in (SPECS_DIR / PILOT_SPEC_DIRNAME, REPO_ROOT / GLOBAL_DIRNAME):
        if scope.exists():
            rebuild_index(scope)
    return report


def protected_dir() -> Path:
    """Contenido sensible o extenso: FUERA del repositorio."""
    base = os.environ.get("FOOTBALL_MARKET_PROTECTED_STORE")
    root = Path(base) if base else (
        Path(os.environ.get("XDG_DATA_HOME", Path.home() / ".local" / "share"))
        / "football-market" / "history-protected")
    root.mkdir(parents=True, exist_ok=True)
    return root


def register_blocked(ex, session, cls, findings):
    store = protected_dir() / f"{ex['interaction_id']}.json"
    payload = {
        "interaction_id": ex["interaction_id"],
        "session_id": session["id"],
        "user_message_id": ex["user_message_id"],
        "spec_detected": [spec_id(n) for n in cls["detected"]],
        "findings": findings,
        "created_at": dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "note": "Contenido NO copiado al repositorio por detección de material "
                "sensible. Requiere revisión humana y decisión explícita.",
    }
    store.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return {"interaction_id": ex["interaction_id"], "store": str(store),
            "findings": findings}


def deposit_full_copy(ex, body_parts):
    store = protected_dir() / f"{ex['interaction_id']}.full.txt"
    store.write_text("\n".join(body_parts), encoding="utf-8")
    return str(store), sha256_bytes(store.read_bytes())


# --------------------------------------------------------------------------
# Indices
# --------------------------------------------------------------------------

INDEX_HEADER = """# {title}

> Índice generado por `tools/evidence-capture.py`. Es documental: **no** modifica
> `tasks.md`, `specs/trello.md`, Trello, ni ningún estado o gate del workflow SDD.

| Intercambio | Fecha (UTC) | Sesión | Tarea(s) | Etapa SDD | Estado de captura | Clasificación | Gate humano | Registro |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |"""


def rebuild_index(scope: Path):
    prompts = scope / "history" / "prompts"
    rows = []
    pending = []
    if prompts.is_dir():
        for f in sorted(prompts.glob("*/*.md")):
            fm = read_frontmatter(f)
            if not fm:
                continue
            relpath = str(f.relative_to(scope)).replace(os.sep, "/")
            row = (f"| `{fm.get('interaction_id','?')}` "
                   f"| {fm.get('created_at','?')} "
                   f"| `{fm.get('session_id','?')}` "
                   f"| {fm.get('tasks','[]') or '—'} "
                   f"| {fm.get('stage','?')} "
                   f"| {fm.get('capture_status','?')} "
                   f"| {fm.get('classification','?')} "
                   f"| {fm.get('gate_human','NO EVALUADO')} "
                   f"| [`{f.name}`]({relpath}) |")
            rows.append(row)
            if fm.get("classification") != "OK":
                loc = fm.get("spec") or "historial global"
                pending.append((fm.get("interaction_id"), fm.get("classification"), loc))
    title = (f"Historial de interacciones — {PILOT_SPEC_DIRNAME.split('-',1)[1].replace('-',' ').upper()}"
             if (scope / "history").exists() and scope.name != GLOBAL_DIRNAME
             else "Historial global de interacciones (SDD, arquitectura, agentes)")
    out = [INDEX_HEADER.format(title=title)]
    out += rows or ["| — | — | — | — | — | — | — | — | — |"]
    if pending:
        out += ["", "## Clasificación pendiente o global (requiere confirmación humana "
            "para asignar ubicación canónica)", "",
                "| Intercambio | Clasificación | Ubicación actual |", "| --- | --- | --- |"]
        out += [f"| `{i}` | {c} | {s} |" for i, c, s in pending]
    out += ["", "---", "",
            "Regenerar con `python3 tools/evidence-capture.py index` (idempotente: "
            "reconstruye desde los archivos existentes, sin duplicar entradas).", ""]
    target = scope / "history" / "index.md"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(out), encoding="utf-8")
    print(f"[ÍNDICE] {rel(target)} ({len(rows)} registros)", file=sys.stderr)


def write_conventions(scope: Path):
    readme = scope / "history" / "README.md"
    if readme.exists():
        return
    readme.write_text(HISTORY_README, encoding="utf-8")


HISTORY_README = """# Convenciones del historial de interacciones

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
"""


def root_index():
    global_root = REPO_ROOT / GLOBAL_DIRNAME
    global_root.mkdir(parents=True, exist_ok=True)
    entries = [
        ("Historial global de interacciones", "history/index.md"),
        ("Índice de evidencias global", "evidence/index.md"),
        (f"Historial de {PILOT_SPEC_DIRNAME}", f"../specs/{PILOT_SPEC_DIRNAME}/history/index.md"),
        (f"Evidencias de {PILOT_SPEC_DIRNAME}", f"../specs/{PILOT_SPEC_DIRNAME}/evidence/index.md"),
    ]
    lines = ["# Evidencias e historial global — Football Market", "",
             "> Punto de entrada del sistema de historial y evidencias (piloto).",
             "> Documental: no altera el workflow SDD, ni estados, ni gates.", "",
             "| Sección | Ruta |", "| --- | --- |"]
    lines += [f"| {t} | [`{p}`]({p}) |" for t, p in entries]
    lines += ["", "## Alcance del piloto", "",
              f"- Escritura permitida: `specs/{PILOT_SPEC_DIRNAME}/**` y `evidencias/**`.",
              "- SPEC-005: reservada para una expansión posterior (no se escribe).",
              "- SPEC-008: excluida (no se lee ni se escribe).",
              "- La base de datos de OpenCode solo se abre en modo lectura (`mode=ro`);",
              "  nunca se copia completa ni se leen las tablas de cuentas y credenciales.", ""]
    (global_root / "index.md").write_text("\n".join(lines), encoding="utf-8")
    print(f"[ÍNDICE] {rel(global_root / 'index.md')}", file=sys.stderr)

    ev = global_root / "evidence" / "index.md"
    ev.parent.mkdir(parents=True, exist_ok=True)
    if not ev.exists():
        ev.write_text(
            "# Evidencias técnicas globales\n\n"
            "> Separate del historial de conversaciones. Los artefactos extensos o con "
            "material sensible no se incorporan a Git de forma automática.\n\n"
            "| Artefacto | Fecha | Origen | Comando | Estado | Ruta |\n"
            "| --- | --- | --- | --- | --- | --- |\n"
            "| — | — | — | — | NOT RUN | — |\n", encoding="utf-8")
        print(f"[ÍNDICE] {rel(ev)}", file=sys.stderr)


# --------------------------------------------------------------------------
# Verificaciones controladas
# --------------------------------------------------------------------------

def verify(conn, session_ids):
    checks = []

    def check(name, ok, detail):
        checks.append((name, "PASS" if ok else "FAIL", detail))

    for sid in session_ids:
        session = load_sessions(conn, None)
        session = [s for s in session if s["id"] == sid]
        if not session:
            check(f"sesión {sid} existe", False, "no encontrada")
            continue
        session = session[0]
        messages = load_messages(conn, sid)
        exchanges = build_exchanges(messages)
        written = []
        for scope in (SPECS_DIR / PILOT_SPEC_DIRNAME, REPO_ROOT / GLOBAL_DIRNAME):
            for iid, path in existing_records(scope).items():
                if read_frontmatter(path).get("session_id") == sid:
                    written.append((iid, path))
        check(f"[{sid}] al menos un prompt presente en la base",
              any(m["type"] == "user" for m in messages),
              f"{sum(1 for m in messages if m['type']=='user')} mensajes user")
        check(f"[{sid}] registros escritos en disco",
              len(written) > 0, f"{len(written)} archivo(s)")
        # literalidad prompt+respuesta
        literal_ok, detail_ok = True, []
        by_id = {i: p for i, p in written}
        for ex in exchanges:
            path = by_id.get(ex["interaction_id"])
            if not path:
                continue
            text = path.read_text(encoding="utf-8")
            if ex["prompt_text"] not in text:
                literal_ok = False
                detail_ok.append(f"prompt ausente en {path.name}")
            for m in ex["assistant_messages"]:
                for vp in m["visible_parts"]:
                    if vp and vp not in text:
                        literal_ok = False
                        detail_ok.append(f"respuesta ausente en {path.name}")
            if "```reasoning" in text or "parts: reasoning" in text:
                literal_ok = False
                detail_ok.append(f"contenido de razonamiento filtrado en {path.name}")
        check(f"[{sid}] prompt y respuestas visibles preservados literalmente",
              literal_ok, "; ".join(detail_ok) or "comparación literal exacta")
        # orden
        order_ok, order_detail = True, ""
        for iid, path in written:
            fm = read_frontmatter(path)
            ids = [x.strip() for x in str(fm.get("assistant_message_ids", "")).split(",") if x.strip()]
            seqs = []
            for aid in ids:
                for m in messages:
                    if m["id"] == aid:
                        seqs.append(m["seq"])
            if seqs != sorted(seqs):
                order_ok = False
                order_detail = f"orden incorrecto en {path.name}"
        check(f"[{sid}] orden original de las respuestas preservado", order_ok,
              order_detail or "seq ascendente en todos los registros")
        # estado
        check(f"[{sid}] estado de captura declarado",
              all(read_frontmatter(p).get("capture_status") for _, p in written),
              ", ".join(sorted({read_frontmatter(p).get("capture_status", "?") for _, p in written})))
    return checks


# --------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser(description="Piloto de historial de interacciones OpenCode")
    sub = ap.add_subparsers(dest="cmd", required=True)

    p_list = sub.add_parser("list", help="lista sesiones disponibles")
    p_list.add_argument("--directory")
    p_list.add_argument("--limit", type=int, default=20)

    p_cap = sub.add_parser("capture", help="captura intercambios")
    p_cap.add_argument("--session", action="append")
    p_cap.add_argument("--last", type=int)
    p_cap.add_argument("--max-exchanges", type=int, default=3)
    p_cap.add_argument("--all", action="store_true", help="todas las sesiones del repo (no recomendado)")

    sub.add_parser("index", help="reconstruye índices")
    sub.add_parser("report", help="reporte de trazabilidad")

    p_ver = sub.add_parser("verify", help="verificaciones controladas de solo lectura")
    p_ver.add_argument("--session", action="append", required=True)

    args = ap.parse_args()
    db = find_db()
    conn = open_readonly(db)

    if args.cmd == "list":
        sessions = load_sessions(conn, args.directory or str(REPO_ROOT))
        for s in sessions[:args.limit]:
            n = sum(1 for _ in load_messages(conn, s["id"]) if _["type"] == "user")
            print(f"{s['id']}  user={n:3d}  {iso(s['time_created'])}  "
                  f"{'sub ' if s['parent_id'] else '    '}{s['title'] or s['slug']}")
        return 0

    if args.cmd == "capture":
        sessions = load_sessions(conn, str(REPO_ROOT))
        if args.session:
            sessions = [s for s in sessions if s["id"] in args.session]
        elif args.last:
            sessions = sessions[:args.last]
        elif not args.all:
            ap.error("indicá --session, --last o --all")
        if not sessions:
            print("No hay sesiones que capturar.")
            return 0
        report = capture_limited(sessions, conn, args.max_exchanges)
        write_conventions(SPECS_DIR / PILOT_SPEC_DIRNAME)
        root_index()
        print("\nResumen:", json.dumps(
            {k: len(v) for k, v in report.items()}, ensure_ascii=False))
        if report["blocked"]:
            print("REVISIÓN HUMANA REQUERIDA: registros bloqueados por material sensible "
                  "(no escritos en el repositorio).")
        if report["pending"]:
            print("CLASIFICACIÓN PENDIENTE: revisar los registros marcados en el índice.")
        return 0

    if args.cmd == "index":
        for scope in (SPECS_DIR / PILOT_SPEC_DIRNAME, REPO_ROOT / GLOBAL_DIRNAME):
            if scope.exists():
                rebuild_index(scope)
        root_index()
        return 0

    if args.cmd == "report":
        rows = []
        for scope in (SPECS_DIR / PILOT_SPEC_DIRNAME, REPO_ROOT / GLOBAL_DIRNAME):
            prompts = scope / "history" / "prompts"
            if not prompts.is_dir():
                continue
            for f in sorted(prompts.glob("*/*.md")):
                fm = read_frontmatter(f)
                rows.append((scope.name, fm))
        if not rows:
            print("Sin registros.")
            return 0
        print("| SPEC | Fecha | Tareas | Etapa | Captura | Clasificación | Gate |")
        print("| --- | --- | --- | --- | --- | --- | --- |")
        for scope_name, fm in rows:
            print(f"| {fm.get('spec','global')} | {fm.get('created_at')} | "
                  f"{fm.get('tasks','[]') or '—'} | {fm.get('stage')} | "
                  f"{fm.get('capture_status')} | {fm.get('classification')} | "
                  f"{fm.get('gate_human')} |")
        return 0

    if args.cmd == "verify":
        checks = verify(conn, args.session)
        width = max(len(c[0]) for c in checks)
        for name, status, detail in checks:
            print(f"{status:4s}  {name.ljust(width)}  {detail}")
        return 0 if all(c[1] == "PASS" for c in checks) else 1

    return 0


def capture_limited(sessions, conn, max_exchanges):
    """Captura acotada: como máximo N intercambios (los más recientes) por sesión."""
    return capture(conn, sessions, max_exchanges)


if __name__ == "__main__":
    sys.exit(main())