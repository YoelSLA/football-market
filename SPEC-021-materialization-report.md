SPEC-021 Materialization Report
================================

Issue: #43 (REQ-S2-14-B | Publicar ventas de tokens desde el frontend)
SPEC Identifier: SPEC-021
Scope: publish-token-sales-frontend
Branch: feat/021-publish-token-sales-frontend
Status: MATERIALIZED (phases 0-1 complete, phases 2-4 pending)

Artifacts Created:
- specs/021-publish-token-sales-frontend/spec.md (full frontend spec with 5 user stories, 11 acceptance criteria, frontend FR-F01 to FR-F08, 14 success criteria SC-001 to SC-014, out of scope, trazability)
- specs/021-publish-token-sales-frontend/ directory
- SPEC-021-preflight-report.md (preflight analysis)

Key Decisions:
- Directory renumbered from 043 (script auto-assign) to 021 (requested SPEC identifier). SPEC-021 was verified available across 4 conflict sources.
- Spec content derived entirely from issue #43 body, CA/RF, SPEC-011 contracts, and mockups 07/08 references.
- No backend implementation included; spec focuses on frontend flow, validation, confirmation, error states, and portfolio integration per SPEC-011 rules.
- All 11 CA from issue #43 mapped to detailed acceptance scenarios with Given/When/Then.
- Out of scope explicitly bounded to prevent scope creep.

Pending Actions (per Spec Kit flow):
1. git push -u origin feat/021-publish-token-sales-frontend
2. GraphQL createLinkedBranch mutation: issueId=#43, oid=origin/dev SHA, name=feat/021-publish-token-sales-frontend
3. gh issue edit 43 --title "SPEC-021 | Publicar ventas de tokens desde el frontend"
4. gh project item-edit for project "Football Market — Development" to set IN PROGRESS
5. Verify issue body/labels/milestone/assignees conservation
6. /speckit.specify for SPECIFY phase

No commits, no code implementation, no test creation. Pure Spec Kit workflow execution.