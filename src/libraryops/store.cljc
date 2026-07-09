(ns libraryops.store
  "SSoT for the community-library/archive actor, behind a `Store`
  protocol so the backend is a swap, not a rewrite -- the same seam
  every prior `cloud-itonami-isic-*` actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/libraryops/store_contract_test.clj), which is the whole
  point: the actor, the Library Governor and the audit ledger never
  know which SSoT they run on.

  Like `adminops`/8411's own `case`, the primary entity here is an
  `item` -- item-lending and item-preservation actuation events apply
  SEQUENTIALLY to the SAME item record (lend first, preserve later),
  matching the freight/quarry/agronomy/hospitality/practice/
  employment/administration cluster's own sequential entity shape.
  Dedicated double-actuation-guard booleans (`:lent?`/`:preserved?`,
  never a `:status` value).

  The ledger stays append-only on every backend: 'which item was
  screened for a lending restriction or a missing conservator sign-
  off, which item was lent, which item was preserved, on what
  jurisdictional basis, approved by whom' is always a query over an
  immutable log -- the audit trail a community library or archive
  trusting an operator needs, and the evidence an operator needs if a
  lending or a preservation is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [libraryops.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (item [s id])
  (all-items [s])
  (assessment-of [s item-id] "committed jurisdiction assessment, or nil")
  (ledger [s])
  (lending-history [s] "the append-only item-lending history (libraryops.registry drafts)")
  (preservation-history [s] "the append-only item-preservation history (libraryops.registry drafts)")
  (next-lending-sequence [s jurisdiction] "next lending-number sequence for a jurisdiction")
  (next-preservation-sequence [s jurisdiction] "next preservation-number sequence for a jurisdiction")
  (item-already-lent? [s item-id] "has this item already been lent?")
  (item-already-preserved? [s item-id] "has this item already been preserved?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-items [s items] "replace/seed the item directory (map id->item)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained item set covering both actuation
  lifecycles (lend, preserve) plus the governor's own new checks, so
  the actor + tests run offline."
  []
  {:items
   {"item-1" {:id "item-1" :title "Kita Local History" :material-type :book
              :days-overdue 5 :daily-rate 20.0 :claimed-late-fee 100.0
              :lending-restricted? false
              :requires-conservator-sign-off? false :conservator-sign-off-obtained? false
              :lent? false :preserved? false
              :jurisdiction "JPN" :status :intake}
    "item-2" {:id "item-2" :title "Atlantis Chronicle" :material-type :book
              :days-overdue 3 :daily-rate 20.0 :claimed-late-fee 60.0
              :lending-restricted? false
              :requires-conservator-sign-off? false :conservator-sign-off-obtained? false
              :lent? false :preserved? false
              :jurisdiction "ATL" :status :intake}
    "item-3" {:id "item-3" :title "Minami Gazette" :material-type :periodical
              :days-overdue 4 :daily-rate 25.0 :claimed-late-fee 150.0
              :lending-restricted? false
              :requires-conservator-sign-off? false :conservator-sign-off-obtained? false
              :lent? false :preserved? false
              :jurisdiction "JPN" :status :intake}
    "item-4" {:id "item-4" :title "Higashi Deposit Copy" :material-type :legal-deposit
              :days-overdue 0 :daily-rate 20.0 :claimed-late-fee 0.0
              :lending-restricted? true
              :requires-conservator-sign-off? false :conservator-sign-off-obtained? false
              :lent? false :preserved? false
              :jurisdiction "JPN" :status :intake}
    "item-5" {:id "item-5" :title "Nishi Illuminated Manuscript" :material-type :rare-manuscript
              :days-overdue 2 :daily-rate 30.0 :claimed-late-fee 60.0
              :lending-restricted? false
              :requires-conservator-sign-off? true :conservator-sign-off-obtained? false
              :lent? false :preserved? false
              :jurisdiction "JPN" :status :intake}
    "item-6" {:id "item-6" :title "Chuo Illuminated Manuscript" :material-type :rare-manuscript
              :days-overdue 1 :daily-rate 30.0 :claimed-late-fee 30.0
              :lending-restricted? false
              :requires-conservator-sign-off? true :conservator-sign-off-obtained? true
              :lent? false :preserved? false
              :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- lend-item!
  "Backend-agnostic `:item/mark-lent` -- looks up the item via the
  protocol and drafts the lending record, and returns {:result ..
  :item-patch ..} for the caller to persist."
  [s item-id]
  (let [i (item s item-id)
        seq-n (next-lending-sequence s (:jurisdiction i))
        result (registry/register-lending item-id (:jurisdiction i) seq-n)]
    {:result result
     :item-patch {:lent? true
                 :lending-number (get result "lending_number")}}))

(defn- preserve-item!
  "Backend-agnostic `:item/mark-preserved` -- looks up the item via
  the protocol and drafts the preservation record, and returns
  {:result .. :item-patch ..} for the caller to persist."
  [s item-id]
  (let [i (item s item-id)
        seq-n (next-preservation-sequence s (:jurisdiction i))
        result (registry/register-preservation item-id (:jurisdiction i) seq-n)]
    {:result result
     :item-patch {:preserved? true
                 :preservation-number (get result "preservation_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (item [_ id] (get-in @a [:items id]))
  (all-items [_] (sort-by :id (vals (:items @a))))
  (assessment-of [_ item-id] (get-in @a [:assessments item-id]))
  (ledger [_] (:ledger @a))
  (lending-history [_] (:lending-records @a))
  (preservation-history [_] (:preservation-records @a))
  (next-lending-sequence [_ jurisdiction] (get-in @a [:lending-sequences jurisdiction] 0))
  (next-preservation-sequence [_ jurisdiction] (get-in @a [:preservation-sequences jurisdiction] 0))
  (item-already-lent? [_ item-id] (boolean (get-in @a [:items item-id :lent?])))
  (item-already-preserved? [_ item-id] (boolean (get-in @a [:items item-id :preserved?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :item/upsert
      (swap! a update-in [:items (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :item/mark-lent
      (let [item-id (first path)
            {:keys [result item-patch]} (lend-item! s item-id)
            jurisdiction (:jurisdiction (item s item-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:lending-sequences jurisdiction] (fnil inc 0))
                       (update-in [:items item-id] merge item-patch)
                       (update :lending-records registry/append result))))
        result)

      :item/mark-preserved
      (let [item-id (first path)
            {:keys [result item-patch]} (preserve-item! s item-id)
            jurisdiction (:jurisdiction (item s item-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:preservation-sequences jurisdiction] (fnil inc 0))
                       (update-in [:items item-id] merge item-patch)
                       (update :preservation-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-items [s items] (when (seq items) (swap! a assoc :items items)) s))

(defn seed-db
  "A MemStore seeded with the demo item set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :lending-sequences {} :lending-records []
                           :preservation-sequences {} :preservation-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (assessment payloads, ledger facts,
  lending/preservation records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses."
  {:item/id                        {:db/unique :db.unique/identity}
   :assessment/item-id             {:db/unique :db.unique/identity}
   :ledger/seq                     {:db/unique :db.unique/identity}
   :lending-record/seq             {:db/unique :db.unique/identity}
   :preservation-record/seq        {:db/unique :db.unique/identity}
   :lending-sequence/jurisdiction      {:db/unique :db.unique/identity}
   :preservation-sequence/jurisdiction  {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- item->tx [{:keys [id title material-type days-overdue daily-rate claimed-late-fee
                         lending-restricted?
                         requires-conservator-sign-off? conservator-sign-off-obtained?
                         lent? preserved?
                         jurisdiction status lending-number preservation-number]}]
  (cond-> {:item/id id}
    title                                       (assoc :item/title title)
    material-type                                  (assoc :item/material-type material-type)
    days-overdue                                      (assoc :item/days-overdue days-overdue)
    daily-rate                                           (assoc :item/daily-rate daily-rate)
    claimed-late-fee                                        (assoc :item/claimed-late-fee claimed-late-fee)
    (some? lending-restricted?)                                (assoc :item/lending-restricted? lending-restricted?)
    (some? requires-conservator-sign-off?)                        (assoc :item/requires-conservator-sign-off? requires-conservator-sign-off?)
    (some? conservator-sign-off-obtained?)                           (assoc :item/conservator-sign-off-obtained? conservator-sign-off-obtained?)
    (some? lent?)                                                       (assoc :item/lent? lent?)
    (some? preserved?)                                                     (assoc :item/preserved? preserved?)
    jurisdiction                                                              (assoc :item/jurisdiction jurisdiction)
    status                                                                       (assoc :item/status status)
    lending-number                                                                  (assoc :item/lending-number lending-number)
    preservation-number                                                              (assoc :item/preservation-number preservation-number)))

(def ^:private item-pull
  [:item/id :item/title :item/material-type :item/days-overdue :item/daily-rate :item/claimed-late-fee
   :item/lending-restricted? :item/requires-conservator-sign-off? :item/conservator-sign-off-obtained?
   :item/lent? :item/preserved?
   :item/jurisdiction :item/status :item/lending-number :item/preservation-number])

(defn- pull->item [m]
  (when (:item/id m)
    {:id (:item/id m) :title (:item/title m) :material-type (:item/material-type m)
     :days-overdue (:item/days-overdue m) :daily-rate (:item/daily-rate m) :claimed-late-fee (:item/claimed-late-fee m)
     :lending-restricted? (boolean (:item/lending-restricted? m))
     :requires-conservator-sign-off? (boolean (:item/requires-conservator-sign-off? m))
     :conservator-sign-off-obtained? (boolean (:item/conservator-sign-off-obtained? m))
     :lent? (boolean (:item/lent? m)) :preserved? (boolean (:item/preserved? m))
     :jurisdiction (:item/jurisdiction m) :status (:item/status m)
     :lending-number (:item/lending-number m) :preservation-number (:item/preservation-number m)}))

(defrecord DatomicStore [conn]
  Store
  (item [_ id]
    (pull->item (d/pull (d/db conn) item-pull [:item/id id])))
  (all-items [_]
    (->> (d/q '[:find [?id ...] :where [?e :item/id ?id]] (d/db conn))
         (map #(pull->item (d/pull (d/db conn) item-pull [:item/id %])))
         (sort-by :id)))
  (assessment-of [_ item-id]
    (dec* (d/q '[:find ?p . :in $ ?iid
                :where [?a :assessment/item-id ?iid] [?a :assessment/payload ?p]]
              (d/db conn) item-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (lending-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :lending-record/seq ?s] [?e :lending-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (preservation-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :preservation-record/seq ?s] [?e :preservation-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-lending-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :lending-sequence/jurisdiction ?j] [?e :lending-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-preservation-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :preservation-sequence/jurisdiction ?j] [?e :preservation-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (item-already-lent? [s item-id]
    (boolean (:lent? (item s item-id))))
  (item-already-preserved? [s item-id]
    (boolean (:preserved? (item s item-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :item/upsert
      (d/transact! conn [(item->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/item-id (first path) :assessment/payload (enc payload)}])

      :item/mark-lent
      (let [item-id (first path)
            {:keys [result item-patch]} (lend-item! s item-id)
            jurisdiction (:jurisdiction (item s item-id))
            next-n (inc (next-lending-sequence s jurisdiction))]
        (d/transact! conn
                     [(item->tx (assoc item-patch :id item-id))
                      {:lending-sequence/jurisdiction jurisdiction :lending-sequence/next next-n}
                      {:lending-record/seq (count (lending-history s)) :lending-record/record (enc (get result "record"))}])
        result)

      :item/mark-preserved
      (let [item-id (first path)
            {:keys [result item-patch]} (preserve-item! s item-id)
            jurisdiction (:jurisdiction (item s item-id))
            next-n (inc (next-preservation-sequence s jurisdiction))]
        (d/transact! conn
                     [(item->tx (assoc item-patch :id item-id))
                      {:preservation-sequence/jurisdiction jurisdiction :preservation-sequence/next next-n}
                      {:preservation-record/seq (count (preservation-history s)) :preservation-record/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-items [s items]
    (when (seq items) (d/transact! conn (mapv item->tx (vals items)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:items ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [items]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-items s items))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo item set -- the Datomic-backed
  analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
