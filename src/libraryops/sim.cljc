(ns libraryops.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean item through
  intake -> jurisdiction assessment -> item lending (escalate/
  approve/commit) -> item preservation (escalate/approve/commit),
  then a SEPARATE clean conservator-required item through the same
  lifecycle (demonstrating the conditional conservator-sign-off check
  passing cleanly), then shows HARD-hold scenarios: a jurisdiction
  with no spec-basis, a late-fee mismatch (verified first), a lending-
  restricted item, and a missing conservator sign-off on a special-
  collection item, a double lending, and a double preservation.

  Like `retailops`/4711's, `freightops`/4920's, `quarryops`/0810's,
  `agronomyops`/0162's, `hospitalityops`/5510's, `practiceops`/7110's,
  `employmentops`/7810's and `adminops`/8411's own new checks, this
  actor's new checks (`lending-restricted-item?`, `conservator-sign-
  off-missing?`) are evaluated directly at `:item/lend`/`:item/
  preserve` time rather than via a separate screening op -- a real
  lending/preservation decision validates legal-deposit status and
  conservator sign-off at the point of the act itself. Each check is
  still exercised directly and independently below, one item per
  HARD-hold scenario, following the SAME 'exercise the failure mode
  directly, never only via a happy-path actuation' discipline
  `parksafety`'s ADR-2607071922 Decision 5 and every sibling since
  establish."
  (:require [langgraph.graph :as g]
            [libraryops.store :as store]
            [libraryops.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :librarian :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== item/intake item-1 (JPN, clean, no conservator required) ==")
    (println (exec-op actor "t1" {:op :item/intake :subject "item-1"
                                  :patch {:id "item-1" :title "Kita Local History"}} operator))

    (println "== jurisdiction/assess item-1 (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :jurisdiction/assess :subject "item-1"} operator))
    (println (approve! actor "t2"))

    (println "== item/lend item-1 (always escalates -- actuation/lend-item) ==")
    (let [r (exec-op actor "t3" {:op :item/lend :subject "item-1"} operator)]
      (println r)
      (println "-- human librarian approves --")
      (println (approve! actor "t3")))

    (println "== item/preserve item-1 (always escalates -- actuation/preserve-item) ==")
    (let [r (exec-op actor "t4" {:op :item/preserve :subject "item-1"} operator)]
      (println r)
      (println "-- human librarian approves --")
      (println (approve! actor "t4")))

    (println "== item/intake item-6 (JPN, clean, conservator required and obtained) ==")
    (println (exec-op actor "t5" {:op :item/intake :subject "item-6"
                                  :patch {:id "item-6" :title "Chuo Illuminated Manuscript"}} operator))

    (println "== jurisdiction/assess item-6 (escalates -- human approves) ==")
    (println (exec-op actor "t6" {:op :jurisdiction/assess :subject "item-6"} operator))
    (println (approve! actor "t6"))

    (println "== item/lend item-6 (always escalates) ==")
    (println (exec-op actor "t6b" {:op :item/lend :subject "item-6"} operator))
    (println (approve! actor "t6b"))

    (println "== item/preserve item-6 (conservator required, obtained -- escalates -- human approves) ==")
    (println (exec-op actor "t7" {:op :item/preserve :subject "item-6"} operator))
    (println (approve! actor "t7"))

    (println "== jurisdiction/assess item-2 (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t8" {:op :jurisdiction/assess :subject "item-2" :no-spec? true} operator))

    (println "== jurisdiction/assess item-3 (escalates -- human approves; sets up the late-fee-mismatch test) ==")
    (println (exec-op actor "t9" {:op :jurisdiction/assess :subject "item-3"} operator))
    (println (approve! actor "t9"))

    (println "== item/lend item-3 (always escalates) ==")
    (println (exec-op actor "t9b" {:op :item/lend :subject "item-3"} operator))
    (println (approve! actor "t9b"))

    (println "== item/preserve item-3 (claimed 150.0 vs recompute 100.0 -> HARD hold) ==")
    (println (exec-op actor "t10" {:op :item/preserve :subject "item-3"} operator))

    (println "== jurisdiction/assess item-4 (escalates -- human approves; sets up the lending-restricted test) ==")
    (println (exec-op actor "t11" {:op :jurisdiction/assess :subject "item-4"} operator))
    (println (approve! actor "t11"))

    (println "== item/lend item-4 (lending-restricted legal-deposit item -> HARD hold) ==")
    (println (exec-op actor "t12" {:op :item/lend :subject "item-4"} operator))

    (println "== jurisdiction/assess item-5 (escalates -- human approves; sets up the conservator-sign-off test) ==")
    (println (exec-op actor "t13" {:op :jurisdiction/assess :subject "item-5"} operator))
    (println (approve! actor "t13"))

    (println "== item/lend item-5 (always escalates) ==")
    (println (exec-op actor "t13b" {:op :item/lend :subject "item-5"} operator))
    (println (approve! actor "t13b"))

    (println "== item/preserve item-5 (conservator required, not obtained -> HARD hold) ==")
    (println (exec-op actor "t14" {:op :item/preserve :subject "item-5"} operator))

    (println "== item/lend item-1 AGAIN (double-lending -> HARD hold) ==")
    (println (exec-op actor "t15" {:op :item/lend :subject "item-1"} operator))

    (println "== item/preserve item-1 AGAIN (double-preservation -> HARD hold) ==")
    (println (exec-op actor "t16" {:op :item/preserve :subject "item-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft lending records ==")
    (doseq [r (store/lending-history db)] (println r))

    (println "== draft preservation records ==")
    (doseq [r (store/preservation-history db)] (println r))))
