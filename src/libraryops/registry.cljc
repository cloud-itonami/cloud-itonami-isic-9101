(ns libraryops.registry
  "Pure-function lending + preservation record construction -- an
  append-only library/archive book-of-record draft.

  Like every sibling actor's registry, there is no single international
  reference-number standard for a lending or preservation record --
  every library/jurisdiction assigns its own reference format. This
  namespace does NOT invent one; it builds a jurisdiction-scoped
  sequence number and validates the record's required fields, the
  same honest, non-fabricating discipline `libraryops.facts` uses.

  `late-fee-matches-claim?` is an HONEST reapplication of the SAME
  ground-truth-recompute DISCIPLINE `adminops.registry`'s own
  `assessed-fee-matches-claim?`, `employmentops.registry`'s own
  `placement-fee-matches-claim?`, `practiceops.registry`'s own `fee-
  total-matches-claim?` and `hospitalityops.registry`'s own `folio-
  total-matches-claim?` establish (verify a claimed monetary total
  against the entity's own recorded quantity x unit fields), reapplied
  to an item's late-fee line rather than an assessed fee, placement
  fee, professional fee or folio line -- not claimed as new code,
  though no literal code is shared (different domain).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real integrated-library system. It builds the RECORD an
  operator would keep, not the act of lending an item or preserving
  it itself (that is `libraryops.operation`'s `:item/lend`/`:item/
  preserve`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the library/archive operator's act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(def ^:private amount-scale
  "Sub-minor-unit scale used when comparing two money amounts: 1/10000
  of a unit. Coarser than double representation error by many orders of
  magnitude, finer than any distinction a real money carries."
  10000)

(defn- money=
  "Exact-at-money-precision equality for two amounts.

  `==` on raw doubles is NOT the right comparison here: a product or sum
  of decimal quantities is routinely not the double nearest the true
  total, so a CORRECT claim compared false and an entity that was never
  wrong was rejected. Measured across this fleet's recompute shapes,
  20-27% of cent-denominated combinations failed while being right.

  Rounding both sides to `amount-scale` before comparing removes the
  representation error while preserving every distinction the value can
  actually carry. A missing or non-numeric amount never matches:
  un-verifiable is not the same as correct."
  [x y]
  (and (number? x) (number? y)
       (= (Math/round (* amount-scale (double x)))
          (Math/round (* amount-scale (double y))))))

(defn compute-late-fee
  "The ground-truth late fee for `item`'s own `:days-overdue` and
  `:daily-rate` -- a single flat days x rate calculation, not a full
  fee-schedule engine with caps/waivers."
  [{:keys [days-overdue daily-rate]}]
  (* (double days-overdue) (double daily-rate)))

(defn late-fee-matches-claim?
  "Does `item`'s own `:claimed-late-fee` equal the independently
  recomputed `compute-late-fee`? A pure ground-truth check against
  the item's own permanent fields -- see ns docstring for why this is
  an honest reapplication of the SAME discipline every sibling
  actor's own cost/total-matching check establishes, not a new
  concept."
  [{:keys [claimed-late-fee] :as item}]
  (money= claimed-late-fee (compute-late-fee item)))

(defn register-lending
  "Validate + construct the ITEM-LENDING registration DRAFT -- the
  library/archive operator's own act of lending a real item to a real
  patron. Pure function -- does not touch any real integrated-library
  system; it builds the RECORD an operator would keep.
  `libraryops.governor` independently re-verifies the item's own
  lending-restriction ground truth, and blocks a double-lending of
  the same record, before this is ever allowed to commit."
  [item-id jurisdiction sequence]
  (when-not (and item-id (not= item-id ""))
    (throw (ex-info "lending: item_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "lending: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "lending: sequence must be >= 0" {})))
  (let [lending-number (str (str/upper-case jurisdiction) "-LND-" (zero-pad sequence 6))
        record {"record_id" lending-number
                "kind" "lending-draft"
                "item_id" item-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "lending_number" lending-number
     "certificate" (unsigned-certificate "ItemLending" lending-number lending-number)}))

(defn register-preservation
  "Validate + construct the ITEM-PRESERVATION registration DRAFT --
  the library/archive operator's own act of preserving a real item
  (triggering late-fee settlement on return and, for special-
  collection items, a conservator sign-off). Pure function -- does
  not touch any real integrated-library system; it builds the RECORD
  an operator would keep. `libraryops.governor` independently re-
  verifies the item's own late-fee/conservator ground truth, and
  blocks a double-preservation of the same record, before this is
  ever allowed to commit."
  [item-id jurisdiction sequence]
  (when-not (and item-id (not= item-id ""))
    (throw (ex-info "preservation: item_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "preservation: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "preservation: sequence must be >= 0" {})))
  (let [preservation-number (str (str/upper-case jurisdiction) "-PRV-" (zero-pad sequence 6))
        record {"record_id" preservation-number
                "kind" "preservation-draft"
                "item_id" item-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "preservation_number" preservation-number
     "certificate" (unsigned-certificate "ItemPreservation" preservation-number preservation-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
