(ns libraryops.registry-test
  (:require [clojure.test :refer [deftest is]]
            [libraryops.registry :as r]))

;; ----------------------------- late-fee-matches-claim? -----------------------------

(deftest matches-when-claim-equals-recompute
  (is (r/late-fee-matches-claim?
       {:days-overdue 5 :daily-rate 20.0 :claimed-late-fee 100.0})))

(deftest mismatches-when-claim-differs-from-recompute
  (is (not (r/late-fee-matches-claim?
            {:days-overdue 4 :daily-rate 25.0 :claimed-late-fee 150.0}))))

(deftest compute-late-fee-is-a-flat-days-times-rate
  (is (= 100.0 (r/compute-late-fee {:days-overdue 5 :daily-rate 20.0}))))

;; ----------------------------- register-lending -----------------------------

(deftest lending-is-a-draft-not-a-real-lending
  (let [result (r/register-lending "item-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest lending-assigns-lending-number
  (let [result (r/register-lending "item-1" "JPN" 7)]
    (is (= (get result "lending_number") "JPN-LND-000007"))
    (is (= (get-in result ["record" "item_id"]) "item-1"))
    (is (= (get-in result ["record" "kind"]) "lending-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest lending-validation-rules
  (is (thrown? Exception (r/register-lending "" "JPN" 0)))
  (is (thrown? Exception (r/register-lending "item-1" "" 0)))
  (is (thrown? Exception (r/register-lending "item-1" "JPN" -1))))

;; ----------------------------- register-preservation -----------------------------

(deftest preservation-is-a-draft-not-a-real-preservation
  (let [result (r/register-preservation "item-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest preservation-assigns-preservation-number
  (let [result (r/register-preservation "item-1" "JPN" 7)]
    (is (= (get result "preservation_number") "JPN-PRV-000007"))
    (is (= (get-in result ["record" "item_id"]) "item-1"))
    (is (= (get-in result ["record" "kind"]) "preservation-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest preservation-validation-rules
  (is (thrown? Exception (r/register-preservation "" "JPN" 0)))
  (is (thrown? Exception (r/register-preservation "item-1" "" 0)))
  (is (thrown? Exception (r/register-preservation "item-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-lending "item-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-lending "item-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-LND-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-LND-000001" (get-in hist2 [1 "record_id"])))))
