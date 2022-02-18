(ns walterl.investec-api.auth-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [java-time :as t]
            [walterl.investec-api.auth :as auth]
            [walterl.investec-api.http :as http]))

(deftest request-access-token!-test
  (testing "access token"
    (let [client-id "client id"
          secret "hunter2"
          header-date (t/offset-date-time 2022 2 17 0 0 0)
          header-date-str (t/format (t/formatter :rfc-1123-date-time) header-date)
          fake-http-post (fn [url opts]
                           {:body (json/generate-string {:url url, :opts opts})
                            :headers {"date" header-date-str}
                            :status 200})]
      (binding [http/*http-post* fake-http-post]
        (let [resp (#'auth/request-access-token! client-id secret)]
          (testing "response is well formed"
            (is (contains? resp :body))
            (is (contains? resp :headers))
            (is (= header-date-str (get-in resp [:headers "date"])))))))))

(deftest access-token-test
  (testing "auth/access-token"
    (let [client-id "client id"
          secret "l3tmein"
          header-date (t/offset-date-time 2022 2 17 0 0 0)
          header-date-str (t/format (t/formatter :rfc-1123-date-time) header-date)
          access-token "ACC3SST0K3N"
          token-type "Bearer"
          expires-in 1799
          scope "scope"
          post-args (atom [])]
      (binding [http/*http-post*
                (fn [url opts]
                  (swap! post-args conj {:url url, :opts opts})
                  {:body (json/generate-string {:access_token access-token
                                                :expires_in expires-in
                                                :scope scope
                                                :token_type token-type})
                   :headers {"date" header-date-str}
                   :status 200})]
        (let [resp (auth/access-token client-id secret)
              args (first @post-args)]
          (testing "returns access token map"
            (is (= {:access-token access-token
                    :token-type token-type
                    :expires-at (t/plus header-date (t/seconds expires-in))
                    :scope scope}
                   resp)))
          (testing "is requested with correct parameters"
            (is (= (#'http/url "/identity/v2/oauth2/token")
                   (:url args)))
            (is (= (#'auth/basic-auth-header client-id secret)
                   (get-in args [:opts :headers])))
            (is (= "grant_type=client_credentials&scope=accounts"
                   (get-in args [:opts :body])))))))))
