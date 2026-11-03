# Specification Quality Checklist: Cotización de tokens de jugadores con historial trazable

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-10-20
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] CHK001 No implementation details (languages, frameworks, APIs)
- [x] CHK002 Focused on user value and business needs
- [x] CHK003 Written for non-technical stakeholders
- [x] CHK004 All mandatory sections completed

## Requirement Completeness

- [x] CHK005 No [NEEDS CLARIFICATION] markers remain
- [x] CHK006 Requirements are testable and unambiguous
- [x] CHK007 Success criteria are measurable
- [x] CHK008 Success criteria are technology-agnostic (no implementation details)
- [x] CHK009 All acceptance scenarios are defined
- [x] CHK010 Edge cases are identified
- [x] CHK011 Scope is clearly bounded
- [x] CHK012 Dependencies and assumptions identified

## Feature Readiness

- [x] CHK013 All functional requirements have clear acceptance criteria
- [x] CHK014 User scenarios cover primary flows
- [x] CHK015 Feature meets measurable outcomes defined in Success Criteria
- [x] CHK016 No implementation details leak into specification

## Notes

- Items marked incomplete require spec updates before `/speckit.clarify` or `/speckit.plan`.
- Validation performed on 2026-10-20 by the Specify phase over `specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md`.
- Specification only; no implementation has been performed and no test has been executed by the agent.
- CHK001: the spec describes capabilities, formulas of the required adjustment model and observable outcomes. The reuse of the existing quotation read port and the removal of the simulated source are stated as required behaviours of the market contract, not as design decisions. The effective HTTP contract is explicitly deferred to the planning phase.
- CHK006: the two decisions with real ambiguity were resolved with the requester on 2026-10-20 and are recorded in the Clarifications section: the effective date of a quote is the date of the valuation used, and a quote is written only when that date is strictly later than the player's current effective quote date, which keeps re-execution idempotent and prevents price jumps on a strategy change.
- CHK009: the five user stories cover initialisation, recalculation with composite re-adjustment, consultation of history/current/date-specific quotes, ranking by current price, and market integration (purchase and sale publication).
- CHK010: the edge cases cover players without metrics, players never valued, valuations older than the current quote, price floor and variation cap, neutral adjustment, re-execution, strategy switch, initialisation in a past date, partial failure and the calculation instant of a consumed quote.
- CHK012: dependencies on SPEC-012 (metrics), SPEC-013 (valuations) and SPEC-010 (quotation read port and market error contract) are stated in the traceability note and in the assumptions.
- CHK015: each success criterion is stated as a measurable percentage or equality that can be checked against persisted data without knowing the implementation.
- Pending outside this checklist: the tracking issue still carries the original title and body of the narrower scope; updating it is an operational-tracking change that requires operational authorisation.
- 2026-10-20 (refinement after the reviewer's question): the spec now states explicitly that a player without a valuation posterior to its current quote is not re-priced (FR-005, FR-027, FR-059), records the late-arriving valuation as an edge case and records the equivalence between a new valuation and a re-evaluation with new metrics in the assumptions and clarifications. No criterion changed its status.
- 2026-10-20 (refinement after the reviewer's question): the vocabulary of the row origin is now closed and explicit, `INITIAL`, `RECOMPUTED` and `CARRIED_FORWARD` (FR-021), with the semantic difference between the composite model, where a carried price means literally no change and an implicit multiplier of 1, and the level model, where a carried price could differ from a recalculated one. A neutral score (0,5) remains `RECOMPUTED`. Requirements were renumbered sequentially (FR-001–FR-060) with the internal cross-references updated. No criterion changed its status.
- 2026-10-20 (alignment with the "Interpretación de cotización en carpeta tp" conversation, reviewed with the requester): the marker name is `RECOMPUTED`; a `CARRIED_FORWARD` row records the strategy that produced the carried price instead of the strategy of the run (FR-019); the adjustment factor keeps the default 0,10 and is constrained to the closed range [0,10; 1,00] with controlled rejection outside it, the variation cap remaining the control that bounds the effective movement (FR-011); the quote queries expose the source valuation date alongside the audit fields (FR-041). The four decisions were registered in the Clarifications section and in the assumptions. No criterion changed its status.
- 2026-10-20 (scope clarification of US5, reviewed with the requester): the purchase and sale-publication flows, the price freezing, the audit of the applied price and the market business errors are already implemented by SPEC-010 and SPEC-011 and are not modified by this feature. US5 now states its three deltas explicitly: the real source behind the existing quotation read port, the retirement of the simulated source, and the controlled rejection of market operations on a market that has not been initialised. Two requirements were added for that rejection, with its own error code distinguishable from the quote-unavailable error (FR-043) and with the initialisation state verifiable through the presence of an `INITIAL` row (FR-044); FR-035 now refers to them. Requirements were renumbered sequentially (FR-001–FR-062) with the internal cross-references updated. No criterion changed its status.
