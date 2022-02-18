(ns walterl.investec-api.auth
  (:require [java-time :as t]
            [walterl.investec-api.http :as http]
            [walterl.investec-api.util :as u]))

(defn- basic-auth-header
  [client-id secret]
  {"Authorization" (str "Basic " (u/base64-encode (str client-id ":" secret)))})

(defn- request-access-token!
  [client-id secret]
  (http/POST nil
             "/identity/v2/oauth2/token"
             "grant_type=client_credentials&scope=accounts"
             {:headers (basic-auth-header client-id secret)}))

(defn- rfc1123->offset-date-time
  [s]
  (t/offset-date-time (t/formatter :rfc-1123-date-time) s))

(defn- safe-rfc1123->offset-date-time
  "Parses date time from RFC1123-formatted `s`tring, or returns \"now\" if it fails."
  [s]
  (or (try
        (rfc1123->offset-date-time s)
        (catch Throwable _))
      (t/offset-date-time)))

(defn- response->access-token
  [{:keys [body headers]}]
  (let [{:keys [access_token expires_in scope token_type]} body]
    {:access-token access_token
     :token-type token_type
     :expires-at (t/plus (safe-rfc1123->offset-date-time (get headers "date"))
                         (t/seconds expires_in))
     :scope scope}))

(defn access-token
  "Obtain an access token.

  https://developer.investec.com/programmable-banking/#get-access-token

      => (access-token \"client-id\" \"secret\")
      {:access-token \"Ms9OsZkyrhBZd5yQJgfEtiDy4t2c\"
       :token-type \"Bearer\"
       :expires-in #object[java.time.OffsetDateTime 0x4304ab94 \"2022-02-17T04:17:54.662519+02:00\"]
       :scope \"accounts\"}
  "
  [client-id secret]
  (response->access-token (request-access-token! client-id secret)))
