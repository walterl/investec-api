(ns walterl.investec-api.util
  (:require [java-time :as t])
  (:import java.nio.charset.StandardCharsets java.util.Base64))

(defn base64-encode
  "Returns base64-encoded string of `s`."
  [s]
  (.encodeToString (Base64/getEncoder) (.getBytes s StandardCharsets/ISO_8859_1)))

(defn iso8601-date-time
  "Format java-time object in ISO-8601 format."
  [d]
  (t/format (t/formatter :iso-offset-date-time) d))
