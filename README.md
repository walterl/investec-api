# Investec API library for Clojure

Clojure wrapper for [Investec's Programmable Banking API](https://developer.investec.com/programmable-banking/).

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.walterl/investec-api.svg)](https://clojars.org/net.clojars.walterl/investec-api)

Add `net.clojars.walterl/investec-api {:mvn/version "0.1.1"}` to your dependencies in `deps.edn`.

## Example

This library is intended for use by other Clojure applications.

```clojure
(require '[walterl.investec-api.accounts :as inv])
(require '[walterl.investec-api.auth :as auth])

(def api-token (auth/access-token "<MY CLIENT ID>" "<MY SECRET>"))

(doseq [{:keys [account-id account-name account-number product-name]} (inv/accounts api-token)
        :let [{:keys [currency current-balance]} (inv/balance api-token account-id)]]
  (println (format "Account %s (%s %s) has %s %.2f"
                   account-name product-name account-number
                   currency (float current-balance))))
```

The above example will print the following for the example data from the API docs:

```
Account Mr John Doe (Private Bank Account 10010206147) has ZAR 28857.76
```

See the [API docs](https://walterl.github.io/investec-api) for more details about available functions.

## Progress

* [X] [Authorization](https://developer.investec.com/programmable-banking/#open-api-authorization)
* [X] [Accounts](https://developer.investec.com/programmable-banking/#open-api-accounts)
* [ ] [Programmable cards API](https://developer.investec.com/programmable-banking/#open-api-programmable-cards-api)

## Development

Run the project's tests:

    $ clj -T:build test

Run the project's CI pipeline and build a JAR:

    $ clj -T:build ci

Install it locally (requires the `ci` task be run first):

    $ clj -T:build install

Generate API docs (outputs to `target/doc`):

    $ clj -X:codox

## License

Copyright © 2022 @walterl

Distributed under the Eclipse Public License version 1.0.
