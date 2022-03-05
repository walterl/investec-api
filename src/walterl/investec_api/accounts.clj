(ns walterl.investec-api.accounts
  (:require [camel-snake-kebab.core :as csk]
            [clojure.set :refer [rename-keys]]
            [walterl.investec-api.http :as http]
            [walterl.investec-api.util :as u]))

;;; Get Accounts

(defn- json->account-map
  [m]
  (rename-keys m
               {:accountId :account-id
                :accountName :account-name
                :accountNumber :account-number
                :referenceName :reference-name
                :productName :product-name}))

(defn accounts
  "Obtain a list of accounts in associated profile.

  `token` as returned by [[walterl.investec-api.auth/access-token]].

  https://developer.investec.com/programmable-banking/#get-accounts

      => (accounts token)
      [{:account-id \"172878438321553632224\",
        :account-name \"Mr John Doe\",
        :account-number \"10010206147\",
        :reference-name \"My Investec Private Bank Account\",
        :product-name \"Private Bank Account\"}]
  "
  [token]
  (-> (http/GET token "/za/pb/v1/accounts")
      (get-in [:body :data :accounts])
      (->> (mapv json->account-map))))

;;; Get Account Balance

(defn- json->balance-map
  [m]
  (rename-keys m
               {:accountId :account-id
                :availableBalance :available-balance
                :currentBalance :current-balance}))

(defn balance
  "Obtain a specified account's balance.

  `token` as returned by [[walterl.investec-api.auth/access-token]].

  https://developer.investec.com/programmable-banking/#get-account-balance

      => (balance token \"172878438321553632224\")
      {:account-id \"172878438321553632224\",
       :current-balance 28857.76,
       :available-balance 98857.76,
       :currency \"ZAR\"}
  "
  [token account-id]
  (-> (http/GET token (str "/za/pb/v1/accounts/" account-id "/balance"))
      (get-in [:body :data])
      json->balance-map))

;;; Get Account Transactions

(defn- txn-opts->query-params
  [{:keys [from-date to-date transaction-type]}]
  (cond-> {}
    from-date (assoc "fromDate" (u/iso8601-date-time from-date))
    to-date (assoc "toDate" (u/iso8601-date-time to-date))
    transaction-type (assoc "transactionType" (some-> transaction-type csk/->PascalCaseString))))

(defn- txn-with-parsed-values
  [m]
  (-> m
      (update :card-number not-empty)
      (update :status #(some-> % csk/->kebab-case-keyword))
      (update :type #(some-> % csk/->kebab-case-keyword))
      (update :transaction-type #(some-> % csk/->kebab-case-keyword))))

(defn- json->txn-map
  [m]
  (-> m
      (rename-keys {:accountId :account-id
                    :actionDate :action-date
                    :cardNumber :card-number
                    :postedOrder :posted-order
                    :postingDate :posting-date
                    :runningBalance :running-balance
                    :transactionDate :transaction-date
                    :transactionType :transaction-type
                    :valueDate :value-date})
      txn-with-parsed-values))

(defn transactions
  "Retrieves all transactions for specified account, optionally filtered by
  `:transaction-type`, `:from-date` and/or `:to-date`.

  `token` as returned by [[walterl.investec-api.auth/access-token]].

  https://developer.investec.com/programmable-banking/#get-account-transactions

      => (transactions token
                       \"172878438321553632224\"
                       {:transaction-type :fees-and-interest
                        :from-date (t/minus (t/local-date-time) (t/months 2))
                        :to-date (t/minus (t/local-date-time) (t/months 1))})
      [{:description \"MONTHLY SERVICE CHARGE\",
        :amount 535.0,
        :running-balance 28857.76,
        :card-number nil,
        :posting-date \"2020-06-11\",
        :type :debit,
        :account-id \"172878438321553632224\",
        :status :posted,
        :action-date \"2020-11-10\",
        :transaction-date \"2020-06-10\",
        :transaction-type :fees-and-interest,
        :value-date \"2020-06-10\",
        :posted-order 13379}
       {:description \"CREDIT INTEREST\",
        :amount 31.09,
        :running-balance 29392.76,
        :card-number nil,
        :posting-date \"2020-06-11\",
        :type :credit,
        :account-id \"172878438321553632224\",
        :status :posted,
        :action-date \"2020-11-10\",
        :transaction-date \"2020-06-10\",
        :transaction-type :fees-and-interest,
        :value-date \"2020-06-10\",
        :posted-order 13378}]
  "
  [token account-id opts]
  (-> (http/GET token
               (str "/za/pb/v1/accounts/" account-id "/transactions")
               {:query-params (txn-opts->query-params opts)})
      (get-in [:body :data :transactions])
      (->> (mapv json->txn-map))))
