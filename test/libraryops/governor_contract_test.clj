(ns libraryops.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls ('lending outside policy is blocked; preservation is
  auditable') implemented faithfully. The single invariant under
  test:

    LibraryOps-LLM never lends or preserves an item the Library
    Governor would reject, `:item/lend`/`:item/preserve` NEVER auto-
    commit at any phase, `:item/intake` (no direct circulation-facing
    risk) MAY auto-commit when clean, and every decision (commit OR
    hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [libraryops.store :as store]
            [libraryops.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :librarian :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  "Walks `subject` through assess -> approve, leaving an assessment on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(defn- lend!
  "Walks `subject` through lend -> approve, leaving :lent? true.
  Assumes `assess!` already ran for this subject."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-lend") {:op :item/lend :subject subject} operator)
  (approve! actor (str tid-prefix "-lend")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :item/intake :subject "item-1"
                   :patch {:id "item-1" :title "Kita Local History"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Kita Local History" (:title (store/item db "item-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "item-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "item-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "item-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "item-1")) "no assessment written"))))

(deftest lend-without-assessment-is-held
  (testing "item/lend before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :item/lend :subject "item-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest lending-restricted-item-is-held-and-unoverridable
  (testing "a lending-restricted legal-deposit item -> HOLD, and never reaches request-approval -- the FLAGSHIP genuinely new check this vertical adds, the 88th unconditional-evaluation-discipline grounding overall, grounded in Japan's own 国立国会図書館法, the US's Copyright Act §407, the UK's Legal Deposit Libraries Act 2003 and Germany's DNBG §16"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "item-4")
          res (exec-op actor "t5" {:op :item/lend :subject "item-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:lending-restricted-item} (-> (store/ledger db) last :basis)))
      (is (empty? (store/lending-history db))))))

(deftest late-fee-mismatch-is-held
  (testing "a claimed late fee that doesn't equal days-overdue x daily-rate -> HOLD (the ground-truth-recompute discipline every sibling's cost/total-matching check establishes)"
    (let [[db actor] (fresh)
          _ (assess! actor "t6pre" "item-3")
          _ (lend! actor "t6pre" "item-3")
          res (exec-op actor "t6" {:op :item/preserve :subject "item-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:late-fee-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/preservation-history db))))))

(deftest conservator-sign-off-missing-is-held-and-unoverridable
  (testing "a missing conservator sign-off on a special-collection item -> HOLD, and never reaches request-approval -- a genuinely new check, the 89th unconditional-evaluation-discipline grounding overall, the FOURTEENTH conditional variant (see this actor's governor ns docstring / the full accumulated ADR-0001 chain: parksafety's ADR-2607071922 Decision 5 through leathergoods's, ictrepair's, retailops's, freightops's, quarryops's, agronomyops's, hospitalityops's, practiceops's, employmentops's and adminops's own)"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "item-5")
          _ (lend! actor "t7pre" "item-5")
          res (exec-op actor "t7" {:op :item/preserve :subject "item-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:conservator-sign-off-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/preservation-history db))))))

(deftest preserve-is-a-noop-when-no-conservator-required
  (testing "the conservator-sign-off check is CONDITIONAL: an item with no conservator-sign-off requirement has no such requirement at all"
    (let [[_db actor] (fresh)
          _ (assess! actor "t7bpre" "item-1")
          _ (lend! actor "t7bpre" "item-1")
          res (exec-op actor "t7b" {:op :item/preserve :subject "item-1"} operator)]
      (is (= :interrupted (:status res)) "clean preservation still escalates for human sign-off, but is NOT a HARD hold"))))

(deftest preserve-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, matching-fee, no-conservator-required preservation still ALWAYS interrupts for human approval -- actuation/preserve-item is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "item-1")
          _ (lend! actor "t8pre" "item-1")
          r1 (exec-op actor "t8" {:op :item/preserve :subject "item-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, preservation record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:preserved? (store/item db "item-1"))))
          (is (= 1 (count (store/preservation-history db))) "one draft preservation record"))))))

(deftest lend-always-escalates-then-human-decides
  (testing "a clean, fully-assessed lending still ALWAYS interrupts for human approval -- actuation/lend-item is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "item-1")
          r1 (exec-op actor "t9" {:op :item/lend :subject "item-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, lending record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:lent? (store/item db "item-1"))))
          (is (= 1 (count (store/lending-history db))) "one draft lending record"))))))

(deftest item-double-lending-is-held
  (testing "lending the same item record twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "item-1")
          _ (lend! actor "t10pre" "item-1")
          res (exec-op actor "t10" {:op :item/lend :subject "item-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-lent} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/lending-history db))) "still only the one earlier lending"))))

(deftest item-double-preservation-is-held
  (testing "preserving the same item twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "item-1")
          _ (lend! actor "t11pre" "item-1")
          _ (exec-op actor "t11a" {:op :item/preserve :subject "item-1"} operator)
          _ (approve! actor "t11a")
          res (exec-op actor "t11" {:op :item/preserve :subject "item-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-preserved} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/preservation-history db))) "still only the one earlier preservation"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :item/intake :subject "item-1"
                          :patch {:id "item-1" :title "Kita Local History"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "item-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
