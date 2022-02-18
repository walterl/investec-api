(ns walterl.investec-api.http
  (:require [cheshire.core :as json]
            [clj-http.lite.client :as http]
            [clojure.string :as str]))

(def ^:private root "https://openapi.investec.com")

(def ^:private ^:dynamic *http-get* http/get)
(def ^:private ^:dynamic *http-post* http/post)

(defn- url
  "Returns API URL for `path`. I.e. it prefixes `root`."
  [path]
  (str root "/" (str/replace-first path #"^/" "")))

(defn- token-auth-header
  [{:keys [access-token token-type]}]
  {"Authorization" (str token-type " " access-token)})

(defn- has-auth-header?
  [opts]
  (some? (get-in opts [:headers "Authorization"])))

(defn- redacted-auth-header
  [opts]
  (update opts :headers assoc "Authorization" "**REDACTED**"))

(defn- safe-query-opts
  [opts]
  (cond-> opts
    (has-auth-header? opts) (redacted-auth-header)))

(defn- request
  [http-method token path query-opts]
  (let [query-opts' (merge query-opts (when token {:headers (token-auth-header token)}))
        {:keys [body headers status] :as resp} (http-method (url path) query-opts')]
    (when (not= 200 status)
      (throw (ex-info "API request failed"
                      {:resp resp
                       :path path
                       :http-method http-method
                       :query-opts (safe-query-opts query-opts')})))
    {:body (json/parse-string body keyword)
     :headers headers
     :status status}))

(defn GET
  "Performs GET request against API, returning parsed and keywordized JSON
  `:body`, `:headers` and `:status`.

  `token` as returned by [[walterl.investec-api.auth/access-token]]."
  [token path & [query-opts]]
  (request *http-get* token path query-opts))

(defn POST
  "Performs POST request against API, returning parsed and keywordized JSON
  `:body`, `:headers` and `:status`.

  `token` as returned by [[walterl.investec-api.auth/access-token]]. May be
  `nil`."
  [token path body & [query-opts]]
  (request *http-post* token path (merge query-opts {:body body})))
