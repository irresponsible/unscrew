The irresponsible clojure guild present...

# unscrew

A really simple clojure library for processing jar files

## Usage

[![Clojars Project](http://clojars.org/irresponsible/unscrew/latest-version.svg)](http://clojars.org/irresponsible/unscrew)

[![Travis CI](https://travis-ci.org/irresponsible/unscrew.svg?branch=master)](https://travis-ci.org/irresponsible/unscrew)

```clj
(require '[irresponsible.unscrew :as u])

(defn get-code-files [jar]
  (u/slurp-jar-matching jar (partial re-find #"\.cl(:?j|js|lj)$") false))
```

For a full manual, consult [resources/Manual.mkdn](resources/Manual.mkdn)

## Clojurescript support

We don't support clojurescript as a host, but you can read clojurescript files
out of your jars. Cljs host support would be difficult, but may happen.

## License

Copyright (c) 2016 James Laver

MIT LICENSE

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
