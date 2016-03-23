The irresponsible clojure guild present...

# unscrew

A really simple clojure library for processing jar files

## Usage

```clj
(require '[irresponsible.unscrew :as u])

(defn get-code-files [jar]
  (u/slurp-jar-matching jar (partial re-find #"\.cl(:?j|js|lj)$") false))
```

## Clojurescript support

We don't support clojurescript as a host, but you can read clojurescript files
out of your jars. Cljs host support would be difficult, but may happen.

## License

Copyright © 2016 James Laver

Distributed under the MIT License (see LICENSE)
