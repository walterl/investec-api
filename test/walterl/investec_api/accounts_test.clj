(ns walterl.investec-api.accounts-test
  (:require [camel-snake-kebab.core :as csk]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [java-time :as t]
            [walterl.investec-api.accounts :as accounts]
            [walterl.investec-api.http :as http]
            [walterl.investec-api.util :as u]))

(deftest get-accounts-test
  (testing "accounts"
    (binding [http/*http-get*
              (fn [_url _opts]
                ;; Response JSON from API docs: https://developer.investec.com/programmable-banking/#get-accounts
                {:body (slurp (io/resource "example_accounts_response.json"))
                 :status 200})]
      (let [accs (accounts/accounts nil)]
        (testing "returns account info map for each account"
          (is (= 1
                 (count accs)))
          (is (= {:account-id "172878438321553632224",
                  :account-name "Mr John Doe",
                  :account-number "10010206147",
                  :reference-name "My Investec Private Bank Account",
                  :product-name "Private Bank Account"}
                 (first accs))))))))

(deftest get-account-balance-test
  (testing "account-balance"
    (let [request-urls (atom [])]
      (binding [http/*http-get*
                (fn [url _opts]
                  (swap! request-urls conj url)
                  ;; Response JSON from API docs: https://developer.investec.com/programmable-banking/#get-account-balance
                  {:body (slurp (io/resource "example_account_balance_response.json"))
                   :status 200})]
        (let [account-id "172878438321553632224"
              balance (accounts/balance nil account-id)]
          (testing "queries the correct URL"
            (is (str/ends-with? (last @request-urls) (str "/" account-id "/balance"))))
          (testing "response includes all expected fields"
            (is (= {:account-id account-id,
                    :current-balance 28857.76,
                    :available-balance 98857.76,
                    :currency "ZAR"}
                   balance))))))))

(deftest get-account-transactions-test
  (testing "account-transactions"
    (let [request-args (atom [])]
      (binding [http/*http-get*
                (fn [url opts]
                  (swap! request-args conj {:url url, :opts opts})
                  ;; Response JSON from API docs: https://developer.investec.com/programmable-banking/#get-account-transactions
                  {:body (slurp (io/resource "example_account_transactions_response.json"))
                   :status 200})]
        (let [account-id "172878438321553632224"
              token {:access-token "ACC3SST0K3N"
                     :token-type "Bearer"}
              txn-type :fees-and-interest
              txn-opts {:transaction-type txn-type
                        :from-date (t/offset-date-time 2020 6 10)
                        :to-date (t/offset-date-time 2020 6 12)}
              txns (accounts/transactions token account-id txn-opts)]
          (testing "queries the correct URL"
            (is (str/ends-with? (:url (last @request-args))
                                (str "/accounts/" account-id "/transactions"))))
          (testing "request has correct auth header"
            (is (= (str (:token-type token) " " (:access-token token))
                   (get-in (last @request-args) [:opts :headers "Authorization"]))))
          (testing "request correctly encodes opts"
            (let [query-params (get-in (last @request-args) [:opts :query-params])]
              (is (= (csk/->PascalCaseString txn-type)
                     (get query-params "transactionType")))
              (is (= (u/iso8601-date-time (:from-date txn-opts))
                     (get query-params "fromDate")))
              (is (= (u/iso8601-date-time (:to-date txn-opts))
                     (get query-params "toDate")))))
          (testing "response includes all expected fields"
            (is (= [{:description "MONTHLY SERVICE CHARGE",
                     :amount 535.0,
                     :running-balance 28857.76,
                     :card-number nil,
                     :posting-date "2020-06-11",
                     :type :debit,
                     :account-id account-id,
                     :status :posted,
                     :action-date "2020-11-10",
                     :transaction-date "2020-06-10",
                     :transaction-type txn-type,
                     :value-date "2020-06-10",
                     :posted-order 13379}
                    {:description "CREDIT INTEREST",
                     :amount 31.09,
                     :running-balance 29392.76,
                     :card-number nil,
                     :posting-date "2020-06-11",
                     :type :credit,
                     :account-id account-id,
                     :status :posted,
                     :action-date "2020-11-10",
                     :transaction-date "2020-06-10",
                     :transaction-type txn-type,
                     :value-date "2020-06-10",
                     :posted-order 13378}]
                   txns))))))))
