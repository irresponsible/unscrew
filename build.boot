
(set-env!
  :project 'irresponsible/unscrew
  :version "0.1.1"
  :resource-paths #{"src" "resources"}
  :source-paths #{"src"}
  :description "A toolkit for processing jar files"
  :url "https://github.com/irresponsible/unscrew"
  :scm {:url "https://github.com/irresponsible/unscrew.git"}
  :developers {"James Laver" "james@seriesofpipes.com"}
  :license {"MIT" "https://en.wikipedia.org/MIT_License"}
  :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                  [byte-streams "0.2.1-alpha1"]
                  [me.raynes/fs     "1.4.6"    :scope "test"]
                  [boot/core "2.5.5"           :scope "test"]
                  [adzerk/boot-test "1.1.0"    :scope "test"]])

;; https://github.com/boot-clj/boot/blob/master/doc/boot.task.built-in.md

(require '[adzerk.boot-test :as t])

(task-options!
  pom {:url         (get-env :url)
       :scm         (get-env :scm)
       :project     (get-env :project)
       :version     (get-env :version)
       :license     (get-env :license)
       :description (get-env :description)
       :developers  (get-env :developers)}
  push {:tag            true
        :ensure-branch  "master"
        :ensure-release true
        :ensure-clean   true
        :gpg-sign       true
        :repositories [["clojars" {:url "https://clojars.org/repo/"}]]}
  target  {:dir #{"target"}})

(deftask tests []
  (set-env! :source-paths #(conj % "test"))
  ;; the tests rely on having the jar kicking about!
  (comp (target) (speak) (t/test)))

(deftask autotest []
  (comp (watch) (tests)))

(deftask make-jar []
  (comp (target) (pom) (jar)))








