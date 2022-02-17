# Investec API library for Clojure

Clojure wrapper for [Investec's Programmable Banking API](https://developer.investec.com/programmable-banking/).

## Usage

Intended for use by other Clojure applications.

Run the project's tests (they'll fail until you edit them):

    $ clojure -T:build test

Run the project's CI pipeline and build a JAR (this will fail until you edit the tests to pass):

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the JAR in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

Install it locally (requires the `ci` task be run first):

    $ clojure -T:build install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables (requires the `ci` task be run first):

    $ clojure -T:build deploy

Your library will be deployed to net.clojars.walterl/investec-api on clojars.org by default.

## License

Copyright © 2022 Walter Leibbrandt

Distributed under the GNU General Public License version 3.
