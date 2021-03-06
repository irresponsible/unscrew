(ns irresponsible.unscrew-test
  (:require [clojure.test :refer :all]
            [me.raynes.fs :as fs]
            [byte-streams :as bs]
            [irresponsible.unscrew :as u])
  (:import [java.util.jar JarFile]))

(deftest unscrew
  (is (= ::finished
         ;; a jar i made of the project some time ago
         (u/with-jar jar "test/project.jar"
           (is (instance? JarFile jar))
           (is (map? (u/get-manifest jar)))
           (is (= "1.0" (get (u/get-manifest jar) "Manifest-Version")))
           (is (= "META-INF/" (first (u/transform-entries jar (map #(.getName %))))))
           (is (= #{"META-INF/" "META-INF/MANIFEST.MF" "irresponsible/"
                    "irresponsible/unscrew.clj" "README.md"}
                  (u/paths jar)
                  (u/entries jar)))
           (is (= #{"META-INF/MANIFEST.MF" "irresponsible/unscrew.clj" "README.md"}
                  (u/files jar)))
           (is (= #{"META-INF/MANIFEST.MF"}
                  (u/paths-matching jar (partial re-find #"MAN"))))
           (is (= #{"irresponsible/unscrew.clj"}
                  (u/clojure-in-jar jar)))
           (is (= #{} (u/classes-in-jar jar)))
           (let [s1 (u/slurp-file jar "META-INF/MANIFEST.MF")
                 s2 (u/slurp-file jar "META-INF/MANIFEST.MF" true)]
             (is (string? s1))
             (is (instance? java.nio.ByteBuffer s2))
             (is (= s1 (bs/to-string s2)))
             (is (not= -1 (.indexOf ^String s1 "Manifest-Version")))
             ::finished)))))
(deftest slurp-jar-matching
  (let [mymap (u/slurp-jar-matching "test/project.jar" #(re-find #".clj$" %) false)]
    (is (= "irresponsible/unscrew.clj" (first (keys mymap))))))
(deftest normalise-class
  (is (= "path.to.foo" (u/normalise-class "path/to/foo.class"))))
(deftest normalise-namespace
  (is (= "path.to.foo" (u/normalise-namespace "path/to/foo.clj"))))
