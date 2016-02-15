(set-env!
  :project 'irresponsible/unscrew
  :version "0.0.1"
  :resource-paths #{"src"}
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0"                  :scope "provided"]
                  [adzerk/boot-test "1.1.0"                     :scope "test"]])

(require '[adzerk.boot-test :as t])

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)
       :description "A toolkit for processing jar files"
       :url "https://github.com/irresponsible/unscrew"
       :scm {:url "https://github.com/irresponsible/unscrew.git"}
       :license {"MIT" "https://en.wikipedia.org/MIT_License"}}
  target  {:dir #{"target"}})

(deftask tests []
  (set-env! :source-paths #(conj % "test"))
  (comp (target)(speak) (t/test)))

(deftask autotest []
  (comp (watch) (tests)))

(deftask make-release-jar []
  (comp (target) (pom) (jar)))
