(ns libraryops.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [libraryops.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "JPN" (:jurisdiction (store/item s "item-1"))))
      (is (= 100.0 (:claimed-late-fee (store/item s "item-1"))))
      (is (false? (:lending-restricted? (store/item s "item-1"))))
      (is (false? (:requires-conservator-sign-off? (store/item s "item-1"))))
      (is (= 150.0 (:claimed-late-fee (store/item s "item-3"))))
      (is (true? (:lending-restricted? (store/item s "item-4"))))
      (is (true? (:requires-conservator-sign-off? (store/item s "item-5"))))
      (is (false? (:conservator-sign-off-obtained? (store/item s "item-5"))))
      (is (true? (:conservator-sign-off-obtained? (store/item s "item-6"))))
      (is (false? (:lent? (store/item s "item-1"))))
      (is (false? (:preserved? (store/item s "item-1"))))
      (is (= ["item-1" "item-2" "item-3" "item-4" "item-5" "item-6"]
             (mapv :id (store/all-items s))))
      (is (nil? (store/assessment-of s "item-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/lending-history s)))
      (is (= [] (store/preservation-history s)))
      (is (zero? (store/next-lending-sequence s "JPN")))
      (is (zero? (store/next-preservation-sequence s "JPN")))
      (is (false? (store/item-already-lent? s "item-1")))
      (is (false? (store/item-already-preserved? s "item-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :item/upsert
                                 :value {:id "item-1" :title "Kita Local History"}})
        (is (= "Kita Local History" (:title (store/item s "item-1"))))
        (is (= 100.0 (:claimed-late-fee (store/item s "item-1"))) "unrelated field preserved"))
      (testing "assessment payloads commit and read back"
        (store/commit-record! s {:effect :assessment/set :path ["item-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/assessment-of s "item-1"))))
      (testing "lending drafts a record and advances the lending sequence"
        (store/commit-record! s {:effect :item/mark-lent :path ["item-1"]})
        (is (= "JPN-LND-000000" (get (first (store/lending-history s)) "record_id")))
        (is (= "lending-draft" (get (first (store/lending-history s)) "kind")))
        (is (true? (:lent? (store/item s "item-1"))))
        (is (= 1 (count (store/lending-history s))))
        (is (= 1 (store/next-lending-sequence s "JPN")))
        (is (true? (store/item-already-lent? s "item-1"))))
      (testing "preservation drafts a record and advances the preservation sequence"
        (store/commit-record! s {:effect :item/mark-preserved :path ["item-1"]})
        (is (= "JPN-PRV-000000" (get (first (store/preservation-history s)) "record_id")))
        (is (= "preservation-draft" (get (first (store/preservation-history s)) "kind")))
        (is (true? (:preserved? (store/item s "item-1"))))
        (is (= 1 (count (store/preservation-history s))))
        (is (= 1 (store/next-preservation-sequence s "JPN")))
        (is (true? (store/item-already-preserved? s "item-1"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/item s "nope")))
    (is (= [] (store/all-items s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/lending-history s)))
    (is (= [] (store/preservation-history s)))
    (is (zero? (store/next-lending-sequence s "JPN")))
    (is (zero? (store/next-preservation-sequence s "JPN")))
    (store/with-items s {"x" {:id "x" :title "t" :material-type :book
                              :days-overdue 1 :daily-rate 1.0 :claimed-late-fee 1.0
                              :lending-restricted? false
                              :requires-conservator-sign-off? false :conservator-sign-off-obtained? false
                              :lent? false :preserved? false
                              :jurisdiction "JPN" :status :intake}})
    (is (= "t" (:title (store/item s "x"))))))
