(ns libraryops.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:item/lend`/`:item/preserve` must NEVER be a member of
  any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [libraryops.phase :as phase]))

(deftest item-lend-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real item lending"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :item/lend))
          (str "phase " n " must not auto-commit :item/lend")))))

(deftest item-preserve-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real item preservation"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :item/preserve))
          (str "phase " n " must not auto-commit :item/preserve")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-circulation-facing-risk-ops
  (testing ":item/intake carries no direct circulation-facing risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:item/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :item/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :item/lend} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :item/preserve} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :item/intake} :commit)))))
