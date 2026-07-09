(ns libraryops.governor
  "Library Governor -- the independent compliance layer that earns
  the LibraryOps-LLM the right to commit. The LLM has no notion of
  jurisdictional legal-deposit/non-circulating-collection or
  professional-conservation-standards law, whether an item's own
  claimed late fee actually equals days overdue times daily rate,
  whether a proposed lending actually involves a legally-restricted
  non-circulating item, whether a rare/special-collection item's own
  preservation treatment has actually received a qualified
  conservator's sign-off, or when an act stops being a draft and
  becomes a real-world item lending or preservation treatment, so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD.

  `:itonami.blueprint/governor` is `:library-governor`, grep-verified
  UNIQUE fleet-wide -- no naming-collision precedent question, a
  fresh independent build following the SAME governed-actor
  architecture (langgraph StateGraph + independent Governor + Phase
  0->3 rollout) established by `cloud-itonami-isic-6511`.

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'lending outside policy is blocked; preservation is
  auditable') and its own docs/operator-guide.md ('handling fragile
  materials and operating in public reading areas' requiring human
  sign-off) name exactly the checks below.

  Seven checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them. The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `libraryops.phase`: for `:stake
  :actuation/lend-item`/`:actuation/preserve-item` (a real lending or
  preservation) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`libraryops.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:item/lend`/`:item/
                                       preserve`, has the jurisdiction
                                       actually been assessed with a
                                       full evidence checklist on
                                       file?
    3. Lending restricted item     -- for `:item/lend`, INDEPENDENTLY
                                       verify the item's own
                                       `:lending-restricted?` is
                                       false -- the FLAGSHIP genuinely
                                       new check this vertical adds
                                       (grep-verified absent fleet-
                                       wide -- zero hits for 'lending-
                                       restricted'/'non-circulating'/
                                       'legal-deposit' as a governor
                                       check function name), the 88th
                                       distinct application of the
                                       unconditional-evaluation
                                       discipline overall (most
                                       recently `adminops.governor/
                                       appeal-rights-notice-missing-
                                       violations` at 87th; a
                                       CONCURRENT session also landed
                                       `construction.governor/permit-
                                       and-inspection-required` on
                                       `cloud-itonami-isic-4211`
                                       earlier in this window, under
                                       its own separate numbering
                                       scheme -- grep-verified no name
                                       collision). Grounded in real
                                       legal-deposit/non-circulating-
                                       collection law: Japan's own
                                       国立国会図書館法 (National Diet
                                       Library Act) Article 24
                                       (納本制度, enforced by the NDL),
                                       the US's Copyright Act 17
                                       U.S.C. §407 (mandatory deposit,
                                       enforced by the Copyright
                                       Office), the UK's Legal Deposit
                                       Libraries Act 2003 (enforced by
                                       the British Library and legal-
                                       deposit libraries), and
                                       Germany's Gesetz über die
                                       Deutsche Nationalbibliothek
                                       (DNBG) §16 (Pflichtablieferung,
                                       enforced by the DNB) --
                                       directly grounded in this
                                       blueprint's own text ('lending
                                       outside policy is blocked').
                                       Evaluated UNCONDITIONALLY (every
                                       lending needs to be checked
                                       against the item's own legal-
                                       deposit/non-circulating status).
    4. Late fee mismatch           -- for `:item/preserve`,
                                       INDEPENDENTLY recompute whether
                                       the item's own `:claimed-late-
                                       fee` equals `days-overdue x
                                       daily-rate`
                                       (`libraryops.registry/late-fee-
                                       matches-claim?`) -- an HONEST
                                       reapplication of the SAME
                                       ground-truth-recompute
                                       DISCIPLINE `adminops.
                                       registry`'s/`employmentops.
                                       registry`'s/`practiceops.
                                       registry`'s own checks
                                       establish, reapplied to an
                                       item's late-fee line -- not
                                       claimed as new.
    5. Conservator sign-off
       missing                        -- for `:item/preserve`, for an
                                       item whose own record declares
                                       `:requires-conservator-sign-
                                       off? true` (i.e. this item is
                                       actually a rare/fragile/
                                       special-collection material
                                       that professionally requires a
                                       qualified conservator's sign-
                                       off before treatment -- not
                                       every item does, routine
                                       repairs of ordinary circulating
                                       items do not), INDEPENDENTLY
                                       check whether `:conservator-
                                       sign-off-obtained?` is true. A
                                       GENUINELY NEW concept (grep-
                                       verified absent fleet-wide --
                                       zero hits for 'conservator-
                                       sign-off'/'conservation-
                                       treatment' as a governor check
                                       function name), the 89th
                                       distinct application overall,
                                       the FOURTEENTH conditional
                                       variant (after
                                       `socialresearch`/7220's,
                                       `bizassoc`/9411's, `training`/
                                       8549's, `furniture`/9524's,
                                       `specialtyrepair`/9529's,
                                       `leathergoods`/9523's,
                                       `ictrepair`/9511's, `quarryops`/
                                       0810's, `agronomyops`/0162's,
                                       `hospitalityops`/5510's,
                                       `practiceops`/7110's,
                                       `employmentops`/7810's and
                                       `adminops`/8411's own, at 63rd,
                                       64th, 66th, 67th, 68th, 69th,
                                       71st, 77th, 79th, 81st, 83rd,
                                       85th and 87th). CONDITIONAL on
                                       the item's own `:requires-
                                       conservator-sign-off?` ground
                                       truth. Grounded in real
                                       professional-conservation-
                                       standards law: Japan's own
                                       文化財保護法 (Act on Protection of
                                       Cultural Properties, enforced
                                       by the Agency for Cultural
                                       Affairs), the US's AIC Code of
                                       Ethics and Guidelines for
                                       Practice (American Institute
                                       for Conservation), the UK's
                                       Icon Professional Standards and
                                       Code of Conduct (Institute of
                                       Conservation), and Germany's
                                       VDR Berufsethische Richtlinien
                                       (Verband der Restauratoren) --
                                       ALL FOUR seeded jurisdictions
                                       actually have a real regime
                                       here, reported honestly (a
                                       full-coverage sub-citation,
                                       matching `quarryops`/0810's own
                                       blast-safety, `agronomyops`/
                                       0162's own water-buffer,
                                       `practiceops`/7110's own
                                       professional-seal,
                                       `employmentops`/7810's own
                                       work-authorization and
                                       `adminops`/8411's own appeal-
                                       rights full coverage rather
                                       than `hospitalityops`/5510's own
                                       honest single-jurisdiction
                                       gap).
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:item/lend`/
                                       `:item/preserve` (REAL acts) ->
                                       escalate.

  Two more guards, double-lending/double-preservation prevention, are
  enforced but NOT listed as numbered HARD checks above because they
  need no upstream comparison at all -- `already-lent-violations`/
  `already-preserved-violations` refuse to lend/preserve the SAME item
  twice, off dedicated `:lent?`/`:preserved?` facts (never a `:status`
  value) -- the SAME 'check a dedicated boolean, not status'
  discipline every prior governor's guards establish, informed by
  `cloud-itonami-isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [libraryops.facts :as facts]
            [libraryops.registry :as registry]
            [libraryops.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Lending a real item and preserving a real item are the two real-
  world actuation events this actor performs -- a two-member set,
  matching every sibling's own dual-actuation shape."
  #{:actuation/lend-item :actuation/preserve-item})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:item/lend`/`:item/preserve`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's legal-deposit/conservation-standards
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :item/lend :item/preserve} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:item/lend`/`:item/preserve`, the jurisdiction's required
  intake/cataloging/lending evidence must actually be satisfied -- do
  not trust the advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:item/lend :item/preserve} op)
    (let [i (store/item st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction i) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(受入記録/目録記録/貸出記録/保存処置承認記録等)が充足していない状態での提案"}]))))

(defn- lending-restricted-item-violations
  "For `:item/lend`, INDEPENDENTLY verify the item's own `:lending-
  restricted?` is false -- the flagship genuinely new check this
  vertical adds. Evaluated UNCONDITIONALLY (every lending needs to be
  checked against the item's own legal-deposit/non-circulating
  status)."
  [{:keys [op subject]} st]
  (when (= op :item/lend)
    (let [i (store/item st subject)]
      (when (true? (:lending-restricted? i))
        [{:rule :lending-restricted-item
          :detail (str subject " は貸出制限資料(納本/永久保存)であり館外貸出不可")}]))))

(defn- late-fee-mismatch-violations
  "For `:item/preserve`, INDEPENDENTLY recompute whether the item's
  own claimed late fee equals days-overdue x daily-rate via
  `libraryops.registry/late-fee-matches-claim?` -- needs no proposal
  inspection or stored-verdict lookup at all, an honest reapplication
  of the same discipline every sibling actor's own cost/total-
  matching check establishes."
  [{:keys [op subject]} st]
  (when (= op :item/preserve)
    (let [i (store/item st subject)]
      (when-not (registry/late-fee-matches-claim? i)
        [{:rule :late-fee-mismatch
          :detail (str subject " の申告延滞料金(" (:claimed-late-fee i)
                      ")が独立再計算値(" (registry/compute-late-fee i) ")と一致しない")}]))))

(defn- conservator-sign-off-missing-violations
  "For `:item/preserve`, for an item whose own record declares
  `:requires-conservator-sign-off? true`, INDEPENDENTLY check whether
  `:conservator-sign-off-obtained?` is true -- a genuinely new
  concept, CONDITIONAL on the item's own `:requires-conservator-sign-
  off?` ground truth (not every item professionally requires a
  conservator's sign-off before treatment)."
  [{:keys [op subject]} st]
  (when (= op :item/preserve)
    (let [i (store/item st subject)]
      (when (and (true? (:requires-conservator-sign-off? i))
                 (not (true? (:conservator-sign-off-obtained? i))))
        [{:rule :conservator-sign-off-missing
          :detail (str subject " は保存修復専門家の承認を要するが未取得 -- 保存処置提案は進められない")}]))))

(defn- already-lent-violations
  "For `:item/lend`, refuses to lend the SAME item record twice, off
  a dedicated `:lent?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :item/lend)
    (when (store/item-already-lent? st subject)
      [{:rule :already-lent
        :detail (str subject " は既に貸出済み")}])))

(defn- already-preserved-violations
  "For `:item/preserve`, refuses to preserve the SAME item twice, off
  a dedicated `:preserved?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :item/preserve)
    (when (store/item-already-preserved? st subject)
      [{:rule :already-preserved
        :detail (str subject " は既に保存処置済み")}])))

(defn check
  "Censors a LibraryOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (lending-restricted-item-violations request st)
                           (late-fee-mismatch-violations request st)
                           (conservator-sign-off-missing-violations request st)
                           (already-lent-violations request st)
                           (already-preserved-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
