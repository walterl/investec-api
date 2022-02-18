(ns walterl.investec-api.http-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [walterl.investec-api.http :as http])
  (:import clojure.lang.ExceptionInfo))

(deftest GET-test
  (testing "GET"
    (let [get-args (atom [])
          fake-http-get (fn [url opts]
                          (swap! get-args conj {:url url, :opts opts})
                          {:body (json/generate-string {:url url})
                           :status 200})]
      (binding [http/*http-get* fake-http-get]
        (let [token {:access-token "ACC3SST0K3N" ; these are the only required fields
                     :token-type "Bearer"}
              query-opts {:x-test-opt "This is a test value"}
              resp (http/GET token "path" query-opts)
              req-opts (:opts (last @get-args))]
          (testing "forwards query opts"
            (is (= query-opts
                   (select-keys req-opts (keys query-opts)))))
          (testing "correctly encodes token auth header"
            (is (= (str (:token-type token) " " (:access-token token))
                   (get-in req-opts [:headers "Authorization"]))
                (prn-str (:headers req-opts))))
          (testing "response includes headers, body and status"
            (is (map? resp))
            (is (contains? resp :headers))
            (is (contains? resp :body))
            (is (contains? resp :status)))
          (testing "parses response JSON"
            (is (map? (:body resp)))
            (is (= #{:url}
                   (set (keys (:body resp))))))))
      (binding [http/*http-get* (fn [_url _opts]
                                  {:error "Test error"
                                   :status 500})]
        (testing "throws on non-200 HTTP status"
          (is (thrown-with-msg? ExceptionInfo #"^API request failed$"
                                (http/GET nil ""))))))))
