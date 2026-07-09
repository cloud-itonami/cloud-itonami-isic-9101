# ADR-0001: LibraryOps-LLM ⊣ Library Governor architecture

## Status

Accepted. `cloud-itonami-isic-9101` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-9101` publishes an OSS business blueprint for
community library and archive operations (cataloging, lending,
preservation and exhibition). Like every prior actor in this fleet,
the blueprint alone is not an implementation: this ADR records the
governed-actor architecture that promotes it to real, tested code,
following the same langgraph StateGraph + independent Governor + Phase
0→3 rollout pattern established by `cloud-itonami-isic-6511` (life
insurance) and applied across 94 prior siblings, most recently
`cloud-itonami-isic-8411` (community public administration service).

A `kotoba-lang` org search for library/archive/ils/catalog/marc/
preservation-named repos returned zero hits. This build returns to
self-contained domain logic, the same pattern the majority of this
fleet's actors use.

This blueprint's own `:itonami.blueprint/governor` keyword,
`:library-governor`, is grep-verified UNIQUE fleet-wide -- no naming-
collision precedent question, a fresh independent build (clean on the
first attempt, unlike `practiceops`/7110's own collision case).

## Decision

### Decision 1: fresh governor identity, no reuse precedent needed

`:library-governor` is grep-verified unique across every
blueprint.edn in this fleet. This build follows the SAME governed-
actor architecture as every prior actor, but with its own distinct
governor identity.

### Decision 2: dual-actuation shape, SEQUENTIAL on the SAME `item` entity

This blueprint's own operating states ("intake : catalog : lend :
preserve : exhibit : audit") and its own Trust Controls ("lending
outside policy is blocked; preservation is auditable") name two
real-world acts: lending an item and preserving an item. These apply
SEQUENTIALLY to the SAME `item` entity -- lend first, preserve later
-- matching `adminops`/8411's, `employmentops`/7810's, `practiceops`/
7110's, `hospitalityops`/5510's, `freightops`/4920's, `quarryops`/
0810's and `agronomyops`/0162's own sequential shape rather than
`retailops`/4711's own alternative-kind shape. `high-stakes` is
`#{:actuation/lend-item :actuation/preserve-item}`.

### Decision 3: `late-fee-matches-claim?` -- an honest reapplication of the ground-truth-recompute discipline

`libraryops.registry/late-fee-matches-claim?` (item's own claimed
late fee vs. days-overdue x daily-rate) applies the SAME discipline
`adminops.registry`'s own `assessed-fee-matches-claim?`,
`employmentops.registry`'s own `placement-fee-matches-claim?` and
`practiceops.registry`'s own `fee-total-matches-claim?` establish --
verify a claimed monetary total against the entity's own recorded
fields, independent of proposal inspection. No literal code is shared
(different domain), but the discipline is the same, documented as
such rather than claimed as a novel invention.

### Decision 4: entity and op shape

The primary entity is an `item`. Four ops: `:item/intake` (directory
upsert, no circulation-facing risk), `:jurisdiction/assess` (per-
jurisdiction legal-deposit/conservation-standards evidence checklist,
never auto), `:item/lend` (POSITIVE, high-stakes), and `:item/
preserve` (POSITIVE, high-stakes).

### Decision 5: `lending-restricted-item?` -- the 88th unconditional-evaluation grounding, the FLAGSHIP genuinely new check

Grep-verified absent fleet-wide (zero hits for `lending-restricted`,
`non-circulating`, `legal-deposit` as a governor check name).
Grounded in real legal-deposit/non-circulating-collection law:
Japan's own 国立国会図書館法 (National Diet Library Act) Article 24 (納本制度,
enforced by the NDL), the US's Copyright Act 17 U.S.C. §407
(mandatory deposit, enforced by the Copyright Office), the UK's Legal
Deposit Libraries Act 2003 (enforced by the British Library and
legal-deposit libraries), and Germany's Gesetz über die Deutsche
Nationalbibliothek (DNBG) §16 (Pflichtablieferung, enforced by the
DNB) -- directly grounded in this blueprint's own text ("lending
outside policy is blocked"). Evaluated UNCONDITIONALLY on every
`:item/lend` (every lending needs to be checked against the item's
own legal-deposit/non-circulating status).

### Decision 6: `conservator-sign-off-missing?` -- the 89th unconditional-evaluation grounding, the FOURTEENTH conditional variant

Before writing this check, every prior sibling's governor namespace
was grepped for any check function named `conservator-sign-off` or
`conservation-treatment` -- zero hits, confirming this is a genuinely
new concept. This is the FOURTEENTH conditional variant (after
`socialresearch`/7220's, `bizassoc`/9411's, `training`/8549's,
`furniture`/9524's, `specialtyrepair`/9529's, `leathergoods`/9523's,
`ictrepair`/9511's, `quarryops`/0810's, `agronomyops`/0162's,
`hospitalityops`/5510's, `practiceops`/7110's, `employmentops`/7810's
and `adminops`/8411's own, at 63rd, 64th, 66th, 67th, 68th, 69th,
71st, 77th, 79th, 81st, 83rd, 85th and 87th) -- CONDITIONAL on the
item's own `:requires-conservator-sign-off?` ground truth: not every
item professionally requires a qualified conservator's sign-off
before treatment, routine repairs of ordinary circulating items do
not. Grounded in real professional-conservation-standards law:
Japan's own 文化財保護法 (Act on Protection of Cultural Properties,
enforced by the Agency for Cultural Affairs), the US's AIC Code of
Ethics and Guidelines for Practice (American Institute for
Conservation), the UK's Icon Professional Standards and Code of
Conduct (Institute of Conservation), and Germany's VDR Berufsethische
Richtlinien (Verband der Restauratoren). ALL FOUR seeded jurisdictions
actually have a real regime here, reported honestly -- a full-coverage
sub-citation, matching `quarryops`/0810's own blast-safety,
`agronomyops`/0162's own water-buffer, `practiceops`/7110's own
professional-seal, `employmentops`/7810's own work-authorization and
`adminops`/8411's own appeal-rights full coverage rather than
`hospitalityops`/5510's own honest single-jurisdiction gap.

### Decision 7: dedicated double-actuation-guard booleans

`:lent?`/`:preserved?` are dedicated booleans on the `item` record,
never a single `:status` value -- the same discipline every prior
governor's guards establish, informed by `cloud-itonami-isic-6492`'s
real status-lifecycle bug (ADR-2607071320).

### Decision 8: Store protocol, MemStore + DatomicStore parity

`libraryops.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in
`test/libraryops/store_contract_test.clj`.

### Decision 9: no bespoke domain capability lib, and no `blueprint.edn` field-sync fixes needed beyond `:optional-technologies`

Verified explicitly this session: no `kotoba-lang/library`,
`kotoba-lang/archive`, `kotoba-lang/ils`, `kotoba-lang/catalog` or
`kotoba-lang/marc`-style bespoke capability library exists. This
repo's `blueprint.edn` had the correct `:required-technologies`
matching the `kotoba-lang/industry` registry's own entry for `"9101"`
exactly, but was MISSING `:optional-technologies [:optimization]`
entirely -- the same gap pattern `agronomyops`/0162's,
`hospitalityops`/5510's, `practiceops`/7110's, `employmentops`/7810's
and `adminops`/8411's own builds found. Fixed cleanly in the same
commit as the `:maturity` flip.

### Decision 10: mock + LLM advisor pair

`libraryops.libraryopsllm` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-lending
an item or auto-preserving it).

## Alternatives considered

- **An unconditional conservator-sign-off check** (applying to every
  preservation regardless of whether the item is actually a rare/
  special-collection material). Rejected: routine repairs of ordinary
  circulating items do not require a conservator's sign-off --
  forcing the check onto every preservation would fabricate a
  requirement.
- **Fabricating a jurisdiction gap** to match `hospitalityops`/5510's
  own single-jurisdiction honesty gap. Rejected: the same honesty
  discipline that forbids fabricating coverage also forbids under-
  reporting it -- all four seeded jurisdictions genuinely have a real
  professional-conservation-standards regime here.

## Consequences

- 96th actor in this fleet (95 implemented before this build).
- Establishes two genuinely NEW unconditional-evaluation-discipline
  checks: `lending-restricted-item?` (FLAGSHIP, 88th distinct
  application overall) and `conservator-sign-off-missing?` (89th
  distinct application overall, the FOURTEENTH conditional variant).
- `MemStore` ‖ `DatomicStore` parity is proven by
  `test/libraryops/store_contract_test.clj`.
- 39 tests / 176 assertions pass; lint is clean; the demo
  (`clojure -M:dev:run`) walks two clean lend+preserve lifecycles (no
  conservator required, conservator required-and-obtained), plus four
  HARD-hold scenarios, end-to-end.
- `blueprint.edn` needed a genuine field-sync fix this time (a
  missing `:optional-technologies [:optimization]` key) in addition
  to the `:maturity` flip.

## References

- `cloud-itonami-isic-6511/docs/adr/0001-architecture.md` (origin of
  the general governed-actor architecture pattern)
- `cloud-itonami-isic-8411/docs/adr/0001-architecture.md` (most recent
  prior sibling, template for this ADR's structure)
- 国立国会図書館法 (National Diet Library Act) Article 24; 文化財保護法 (Act on
  Protection of Cultural Properties) (Japan)
- Copyright Act, 17 U.S.C. §407; AIC Code of Ethics and Guidelines
  for Practice (US)
- Legal Deposit Libraries Act 2003; Icon Professional Standards and
  Code of Conduct (UK)
- Gesetz über die Deutsche Nationalbibliothek (DNBG) §16; VDR
  Berufsethische Richtlinien (Germany)
