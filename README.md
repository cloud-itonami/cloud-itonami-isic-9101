# cloud-itonami-isic-9101

Open Business Blueprint for **ISIC Rev.5 9101**: library and archive
operations -- cataloging, lending, preservation and exhibition.

This repository publishes a community-library/archive actor -- item
intake, per-jurisdiction legal-deposit/conservation-standards
regulatory assessment, item lending and item preservation -- as an
OSS business that any qualified operator can fork, deploy, run,
improve and sell, so a community library or archive never surrenders
circulation and preservation data to a closed ILS SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet (95 prior actors) -- here it is
**LibraryOps-LLM ⊣ Library Governor**. This blueprint's own
`:itonami.blueprint/governor` keyword, `:library-governor`, is a
UNIQUE keyword fleet-wide (grep-verified: no other blueprint declares
it) -- a fresh, independent build.

> **Why an actor layer at all?** An LLM is great at drafting an item
> summary, normalizing records, and checking whether a claimed late
> fee actually equals an item's own recorded days overdue times daily
> rate -- but it has **no notion of which jurisdiction's legal-
> deposit/conservation-standards law is official, no license to lend
> a real item or preserve a real item, and no way to know on its own
> whether a proposed lending actually involves a legally-restricted
> non-circulating item or whether a rare/special-collection item's
> own preservation treatment has actually received a qualified
> conservator's sign-off**. Letting it lend or preserve directly
> invites fabricated regulatory citations, a late-fee mismatch being
> charged to a patron, a legal-deposit item being lent out in
> violation of statute, and irreversible conservation treatment being
> applied to a rare item without a qualified conservator's sign-off --
> exposing the operator to real regulatory liability and cultural
> heritage to real, irreversible harm. This project seals the
> LibraryOps-LLM into a single node and wraps it with an independent
> **Library Governor**, a human **approval workflow**, and an
> immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers item intake through legal-deposit/conservation-
standards regulatory assessment, item lending and item preservation.
It does **not**, by itself, hold any operating authority required to
run a library or archive in a given jurisdiction, and it does not
claim to. It also does not perform the actual physical conservation
treatment work itself, or judge preservation quality --
`libraryops.registry/late-fee-matches-claim?` is a pure ground-truth
recompute against the item's own recorded fields, not a treatment-
quality judgment. Whoever deploys and operates a live instance (a
qualified librarian/archivist/conservator) supplies any jurisdiction-
specific authority, the real integrated-library-system integration
and the real conservation-lab integrations, and bears that
jurisdiction's liability -- the software supplies the governed, spec-
cited, audited execution scaffold so that operator does not have to
build the compliance layer from scratch.

### Actuation

**Lending a real item and preserving a real item are never
autonomous, at any phase, by construction.** Two independent layers
enforce this (`libraryops.governor`'s `:actuation/lend-item`/
`:actuation/preserve-item` high-stakes gate and `libraryops.phase`'s
phase table, which never puts either op in any phase's `:auto` set)
-- see `libraryops.phase`'s docstring and `test/libraryops/phase_
test.clj`'s `item-lend-never-auto-at-any-phase`/`item-preserve-never-
auto-at-any-phase`. The actor may draft, check and recommend; a human
librarian/archivist/conservator is always the one who actually lends
an item or preserves it. Grounded directly in this blueprint's own
`docs/business-model.md` Trust Controls text ("lending outside policy
is blocked; preservation is auditable") -- a genuine DUAL-actuation
shape, applied SEQUENTIALLY to the SAME item record (lend first,
preserve later), matching `adminops`/8411's, `employmentops`/7810's,
`practiceops`/7110's, `hospitalityops`/5510's, `freightops`/4920's,
`quarryops`/0810's and `agronomyops`/0162's own sequential shape
rather than `retailops`/4711's own alternative-kind shape.

## The core contract

```
item intake + jurisdiction facts (libraryops.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ LibraryOps-LLM        │ ─────────────▶ │ Library Governor              │  (independent system)
   │ (sealed)              │  + citations    │ spec-basis · evidence-       │
   └───────────────────────┘                 │ incomplete · lending-         │
          │                 commit ◀┼ restricted-item (FLAGSHIP NEW) ·      │
          │                         │ late-fee-mismatch (ground-truth)      │
    record + ledger        escalate ┼ · conservator-sign-off-missing            │
          │              (ALWAYS for│ (conditional, NEW) · already-lent ·       │
          │       :actuation/lend-  │ already-preserved                         │
          │       item/             │                                            │
          │       :actuation/       │                                            │
          │       preserve-item}     │                                            │
          ▼                          └───────────────────────┘
      human approval
```

**The LibraryOps-LLM never lends or preserves an item the Library
Governor would reject, and never does so without a human sign-off.**
Hard violations (fabricated regulatory requirements; unsupported
evidence; a lending-restricted item; a late-fee mismatch; a missing
conservator sign-off on a special-collection item; a double lending/
preservation) force **hold** and *cannot* be approved past; a clean
lending/preservation proposal still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk two clean lend+preserve lifecycles (no conservator required, conservator required-and-obtained), plus four HARD-hold cases, through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a stack and retrieval robot
performs shelving, retrieval and preservation tasks in the stacks,
under the actor, gated by the independent **Library Governor**. The
governor never dispatches hardware itself; `:high`/`:safety-critical`
actions (such as handling fragile materials and operating in public
reading areas) require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Library Governor, lending/preservation draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`9101`). This vertical's item/circulation records are practice-
specific rather than a shared cross-operator data contract, so
`libraryops.*` runs on the generic robotics/identity/forms/dmn/bpmn/
audit-ledger stack only -- no bespoke domain capability lib to
reference at all (unlike `retailops`/4711's own `kotoba-lang/retail`
and `freightops`/4920's own `kotoba-lang/logistics` integrations; a
`kotoba-lang` org search for library/archive/ils/catalog/marc/
preservation-named repos returned zero hits, matching `quarryops`/
0810's, `agronomyops`/0162's, `hospitalityops`/5510's, `practiceops`/
7110's, `employmentops`/7810's and `adminops`/8411's own investigated-
and-ruled-out precedent).

## Layout

| File | Role |
|---|---|
| `src/libraryops/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + lending AND preservation history (dual history). The double-actuation guard checks dedicated `:lent?`/`:preserved?` booleans rather than a `:status` value |
| `src/libraryops/registry.cljc` | Lending/preservation draft records, plus `late-fee-matches-claim?` -- an honest reapplication of the SAME ground-truth-recompute discipline every sibling actor's own cost/total-matching check establishes |
| `src/libraryops/facts.cljc` | Per-jurisdiction legal-deposit AND professional-conservation catalog with an official spec-basis citation per entry, honest coverage reporting -- ALL FOUR seeded jurisdictions have a conservation-standards sub-citation here |
| `src/libraryops/libraryopsllm.cljc` | **LibraryOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/jurisdiction-assessment/lending/preservation proposals |
| `src/libraryops/governor.cljc` | **Library Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · lending-restricted-item, FLAGSHIP NEW, the 88th unconditional-evaluation-discipline grounding · late-fee-mismatch · conservator-sign-off-missing, CONDITIONAL, the 89th grounding) + 2 double-actuation guards + 1 soft (confidence/actuation gate) |
| `src/libraryops/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted assess → supervised (lend/preserve always human; item intake is the ONLY auto-eligible op, no direct circulation-facing risk) |
| `src/libraryops/operation.cljc` | **OperationActor** -- langgraph StateGraph |
| `src/libraryops/sim.cljc` | demo driver |
| `test/libraryops/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers item intake through legal-deposit/conservation-
standards regulatory assessment, item lending and item preservation
-- the core governed lifecycle this blueprint's own `docs/
business-model.md` names in its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Item intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:item/intake`/`:jurisdiction/assess`) | Real integrated-library-system integration, real preservation-quality judgment (see `libraryops.facts`'s docstring) |
| Item lending, HARD-gated on full evidence and legal-deposit/non-circulating compliance, plus a double-lending guard (`:actuation/lend-item`) | |
| Item preservation, HARD-gated on full evidence, a matching late-fee claim and (when applicable) a conservator sign-off, plus a double-preservation guard (`:actuation/preserve-item`) | |
| Immutable audit ledger for every intake/assessment/lending/preservation decision | |

Extending coverage is additive: add the next gate (e.g. an
exhibition-loan-agreement-verification check) as its own governed op
with its own HARD checks and tests, following the SAME "an
independent governor re-verifies against the actor's own records
before any real-world act" pattern this repo's flagship ops already
establish.

## Jurisdiction coverage (honest)

`libraryops.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `libraryops.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `libraryops.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger. Note that the professional-conservation-
standards sub-citation is FULL coverage rather than a gap: ALL FOUR
seeded jurisdictions (JPN, USA, GBR, DEU) actually have a real
professional-conservation-standards enforcement regime, reported
honestly.

## Maturity

`:implemented` -- `LibraryOps-LLM` + `Library Governor` run as real,
tested code (see `Run` above), promoted from the originally-published
`:blueprint`-tier scaffold, following the SAME governed-actor
architecture as the 95 other prior actors across this fleet, with its
own distinct, independently-named governor. See
`docs/adr/0001-architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
