# SPEC-021 Preflight Report — Issue #43

**Date**: 2026-10-26  
**Issue**: #43 — `REQ-S2-14-B | Publicar ventas de tokens desde el frontend`  
**SPEC Identifier**: SPEC-021  
**Scope (derived)**: `publish-token-sales-frontend`  
**Branch type**: `feat` (default)

---

## 0.1 — Arguments

- Issue number: **43** (positive integer ✓)
- SPEC identifier: **SPEC-021** (matches `^SPEC-[0-9]{2,3}$` ✓)
- Scope: derived from issue title in English kebab-case

---

## 0.2 — Issue Analysis

**State**: `OPEN` ✓  
**Title**: `REQ-S2-14-B | Publicar ventas de tokens desde el frontend`  
**Labels**: 🟠 Prioridad Media, 🎨 Frontend  
**Milestone**: Sprint 2 (due 2026-11-03)  
**Assignee**: YoelSLA  

**Functional Requirements (RF)**:
- RF-01: Access to publication — authenticated user can initiate token sale
- RF-02: Available information — show player and available token quantity
- RF-03: Offer data — indicate amount and price conditions
- RF-04: Summary and confirmation — review conditions before sending
- RF-05: Validations — communicate rejections for invalid quantity, insufficient holding, disallowed conditions
- RF-06: Result — clear confirmation of created publication or understandable rejection
- RF-07: Holdings state — reflect available/reserved tokens after successful publication
- RF-08: Usability — accessible, clear, adaptable to screen sizes

**Acceptance Criteria (CA)**:
- CA-01: Authenticated user can initiate sale for a player they possess
- CA-02: Interface identifies player and presents available holding
- CA-03: User can indicate valid amount and allowed price conditions
- CA-04: Summary of publication conditions shown before confirmation
- CA-05: Canceling process generates no publication
- CA-06: Publication only requested after explicit user confirmation
- CA-07: Validation errors/rejections communicated without exposing sensitive info
- CA-08: Successful publication confirms and updates visible availability
- CA-09: Interface distinguishes publication from executed sale (publish ≠ transfer)
- CA-10: During sending, inform request state and avoid unintentional duplicate publications
- CA-11: Usable on different device sizes and with accessible interactions

**Out of Scope** (from issue body):
- Implement token reservation rules, price fixing, or backend persistence
- Execute purchases by other users
- Direct token purchases from superuser
- Cancellation, modification, or general order administration
- Personal portfolio consultation or operation history

**Functional Dependencies**:
- **Bloqueante**: REQ-S2-07 — Venta de tokens de un jugador (#50) — **CLOSED** ✓
- **No bloqueante**: REQ-S2-09 — Consultar portfolio personal (#20) — **CLOSED** ✓

---

## 0.3 — Idempotencia Check

Title does **not** start with `SPEC-<nnn> |`, so no conflict. Proceed with fresh SPEC creation.

---

## 0.4 — Dependencies Verification

| Dependency | Issue | State | Character |
|---|---|---|---|
| REQ-S2-07 | #50 | CLOSED | Bloqueante (closed, satisfies prerequisite) |
| REQ-S2-09 | #20 | CLOSED | No bloqueante (related, consumed) |

Both prerequisites are closed. No blocking dependencies remain.

---

## 0.5 — SPEC-021 Identifier Availability

Checked four sources:

| Source | Result |
|---|---|
| **Local specs** (`ls specs/ | grep -E '^021-'`) | No directory exists ✓ |
| **Remote branches** (`git ls-remote --heads origin | grep -E '/021-'`) | No branches exist ✓ |
| **GitHub issues** (`gh issue list --search "SPEC-021 in:title"`) | No matches ✓ |
| **Project items** | No conflict detected ✓ |

**Conclusion**: SPEC-021 is **available** for use. No conflicts found.

---

## 0.6 — Git State

```
$ git status --short
 M referencias-imagenes/Bundesliga-League.png
 M referencias-imagenes/Ligue 1-League.png
 M "referencias-imagenes/Panel de Cartas Futbolísticas de Ílite.png"
 M referencias-imagenes/Premier League-League.png
 M referencias-imagenes/Primera Division-League.png
 M referencias-imagenes/Serie A-League.png
 M referencias-imagenes/football_market_referencia_dashboard.png
?? referencias-imagenes/07-mercado-vender.png
?? referencias-imagenes/08-mercado-vender-modal-abierto.png

$ git branch
* dev
  main
```

- On `dev` branch, up to date with `origin/dev` ✓
- No tracked changes pending (only untracked new files and some pre-existing deletions) ✓
- `git checkout dev && git merge --ff-only origin/dev` would succeed (already there) ✓

---

## 0.7 — Scope Resolution

**Original title (after prefix removal)**: "Publicar ventas de tokens desde el frontend"  
**Translated to English**: "Publish token sales from frontend"  
**Normalized to kebab-case English**: `publish-token-sales-frontend`  

Validation against `^[a-z0-9]+(-[a-z0-9]+)*$`: ✓  
No Spanish stop words remaining (de, del, la, el, etc. all translated) ✓  

**Result**: Scope = `publish-token-sales-frontend` (derived, not provided verbatim)

---

## 0.8 — Branch Type

Default type is `feat` — this is a new frontend feature implementation ✓

---

## 1.1 — SPEC Materialization (Phase 1)

**Status**: Not executed per user instruction ("No implementes todavía").  
The Spec Kit create-new-feature script would generate:
- Directory: `specs/021-publish-token-sales-frontend/`
- File: `specs/021-publish-token-sales-frontend/spec.md` (from template)
- Branch: `feat/021-publish-token-sales-frontend`
- SPEC_ACTION: `created`
- FEATURE_NUM: `021`

---

## 2 — Issue & Tracking (Phase 3)

**Title change required**: For REQ- issues, title should become `SPEC-021 | Publicar ventas de tokens desde el frontend`  
(Note: original title preserved after code prefix removal)

**Project association**: Issue is already in project "Football Market — Development" (project #4)  
- Current item status: 📋 BACKLOG  
- Next step: Move to 🔨 IN PROGRESS when work begins

---

## 3 — Risks and Next Steps

### Risks
1. **Visual design consistency**: Mockups 07-mercado-vender.png and 08-mercado-vender-modal-abierto.png must be respected for layout, component placement, and user flow. Frontend team must review against existing MarketBuyPage components.
2. **Dependency on SPEC-011**: The backend contracts and rules are already defined in SPEC-011; the frontend implementation must align with `POST /api/orders/sell` contract and associated error codes (404 PLAYER_NOT_FOUND, 404 PLAYER_NOT_VISIBLE, 409 INSUFFICIENT_STOCK, etc.).
3. **Portfolio integration**: Must integrate with existing portfolio consultation (SPEC-014 / REQ-S2-09) to reflect token availability after publication (RF-07, CA-08).
4. **Existing similar feature**: SPEC-019 (issue #42) implements "Comprar tokens desde el frontend" — sell functionality must be distinct but follow similar patterns.

### Next Steps (per Spec Kit flow)
1. **Execute Phase 1**: Run `.specify/scripts/bash/create-new-feature.sh --json --number 43 --short-name 'publish-token-sales-frontend' --type feat 'REQ-S2-14-B | Publicar ventas de tokens desde el frontend'` to materialize the SPEC and create the branch.
2. **Execute Phase 2**: Publish the branch and associate it with issue #43 via GraphQL `createLinkedBranch`.
3. **Execute Phase 3**: Edit issue title to `SPEC-021 | Publicar ventas de tokens desde el frontend`, update project status to IN PROGRESS.
4. **Execute Phase 4**: Report final outcome and prepare for SPECIFY phase (`/speckit.specify`).

### Pending Decisions
- **Frontend component design**: Should the sell publication use existing MarketBuyPage infrastructure or create a new page/component? Mockups should guide this decision.
- **HTTP contract alignment**: Confirm the exact request/response format for `POST /api/orders/sell` from SPEC-011 FR-001 and related requirements.
- **Mockup review**: Design team should validate that the mockups accurately represent the intended UI before development begins.

---
*This report results from Phase 0 (Preflight) of the Spec Kit workflow. No implementation code has been modified, no tests created, and no SPEC file written. The next authorized step is `/speckit.specify` to begin SPECIFY.*