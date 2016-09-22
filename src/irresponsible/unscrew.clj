;; # unscrew
;;
;; ## A really simple clojure library for processing JAR files
;;
;; `unscrew` contains a collection of straight forward utilities for doing
;; basic IO operations with standard Java JAR files, and
;; extracting/interpreting their contents.
;;
;; ## Cheatsheet
;;
;; ## Basic Anatomy of a Jar File
;; A Jar file is mostly just a glorified Zip file decorated with a `MANIFEST`
;; ( which is itself, a file in the Zip, stored in `META-INF/MANIFEST.MF` )
;;
;; This manifest is just a dumb key-value store that can hold arbitrary
;; strings, some of which may be used for various java purposes.
;;
;; ## Basic Examples
;;
;; ### Reading a Jar file and retrieving a manifest entry
;;
;; This example defines a function that will retrieve the manifest
;; entry for the key "foo" in the given Jar file.
;;
;; ```clojure
;;
;; (require '[irresponsible.unscrew :as u])
;;
;; (defn manifest-foo [jar]
;;    (key 'foo'
;;      (u/get-manifest
;;        (u/open-jar jar))))
;;
;; ```
;;
;; ### Reading a Jar file and fetching a binary map of wanted files
;;
;; This example will create a function that will retrieve a map of
;; all clojure or clojurescript source files from the given Jar file as a
;; `{name content}` map.
;;
;; ```clojure
;; (require '[irresponsible.unscrew :as u])
;;
;; (defn jar-source-files [jar]
;;      (u/slurp-jar-matching
;;        jar
;;        (partial re-find #"\.cl(:?j|js|ljs)$")
;;        false))
;;
;; ```
;;
(ns irresponsible.unscrew
  (:require [byte-streams :refer [to-byte-buffer]]
            [clojure.main :refer [demunge]]
            [clojure.string :as s])
  (:import [java.util.jar JarFile JarEntry]))

(defn- mapassoc-kv-with [kf vf init coll]
  (reduce-kv (fn [acc k v]
               (assoc acc (kf k) (vf v))) init coll))

(defn- stringify-map [m]
  (mapassoc-kv-with str str {} m))

;; ## Functions
;;
;; ### open-jar
;; **Low Level**: Opens a Jar file specified by `path` and returns an
;; instance of `java.util.jar.JarFile`
;;
;; Generally you want [with-jar](#with-jar)
;;
;; ```clojure
;; (open-jar path)
;; ```
;;
;;; (open-jar "path/to/jar")
(defn open-jar
  "Opens jar file at given path
   args: [path]`
   returns: JarFile"
  [path]
  (JarFile. path))

;; ### close-jar
;; **Low Level**: Closes a `java.util.jar.JarFile`
;;
;; Generally you want [with-jar](#with-jar)
;;
;; ```clojure
;; (let [jar (open-jar "path/to/jar")]
;;      ...
;;      (close-jar jar))
;; ```
;;
;;; (close-jar (open-jar "path/to/jar"))
(defn close-jar
  "Closes the given JarFile
   args: [jar]"
  [^JarFile jar]
  (.close jar))

;; ### get-manifest
;; Returns the contents of the given Jar files MANIFEST as a `{string string}`
;; map.
;;
;; ```clojure
;; (println (with-jar myjar "path/to/myjar.jar"
;;  (get (get-manifest myjar) "Manifest-Version")
;; ))
;; ```
;;
;;; (get (get-manifest (open-jar "path/to/jar")) "manifestkey")
(defn get-manifest
  "Returns the manifest for the jar as a map
   args: [jar]
   returns: map of string to string"
  [^JarFile jar]
  (when-let [m (.getManifest jar)]
    (when-let [as (.getMainAttributes m)]
      (when-let [es (.entrySet as)]
        (stringify-map (into {} es))))))

(defn- get-name [je]
  (.getName je))

;; ### transform-entries
;; Returns a sorted set of file paths that exist in the jar by applying a
;; transducer to the sequence of `java.util.jar.JarEntry`'s.
;;
;; ```clojure
;; (defn get-name [^JarEntry e]
;;    (.getName e)
;; )
;; (map println (with-jar myjar "path/to/jar"
;;       (transform-entries myjar (map get-name))
;; ))
;; ```
;;
;;; (transform-entries (open-jar "path/to/jar") (map get-name))
(defn transform-entries
  "Returns a sorted-set of filepaths in the jar by applying a transducer to the
   sequence of entries
   args: [jar xf]
   returns: set of string"
  [^JarFile jar xf]
  (->> jar .entries enumeration-seq
       (into (sorted-set) xf)))


;; ### paths
;; Returns a sorted set of all the names of all the file paths present in the
;; Jar.
;;
;; ```clojure
;; (map println (with-jar myjar "path/to/jar"
;;       (paths myjar)
;; ))
;; ```
;;
;;; (paths (open-jar "path/to/jar"))
(defn paths
  "Returns a sorted-set of filepaths in the jar
   args: [jar]
   returns: set of string"
  [jar]
  (transform-entries jar (map get-name)))

(def entries paths) ;; compat

;; ### paths-matching
;; Returns the sorted subset of file paths present in the Jar that satisfy a
;; predicate.
;;
;; ```clojure
;; (defn jar-jpegs [jar]
;;       (paths-matching jar #(re-find #"\.jpg")))
;;
;; (with myjar "path/to/jar" (map println (jar-jpegs myjar)))
;; ```
;;
;;; (paths-matching (open-jar "path/to/jar") #(re-find #"\.clj$" %))
(defn paths-matching
  "Returns a sorted-set of filepaths in the jar for which (pred %) returns truthy
   args: [jar pred]
   returns: set of string"
  [jar pred]
  (transform-entries jar (comp (map get-name) (filter pred) )))

(def entries-matching paths-matching) ;; compat

;; ### files
;; Returns the sorted subset of file paths which are files ( as opposed to
;; either files or directories like 'paths' )
;;
;; ```clojure
;; (with myjar "path/to/jar" (map println (files myjar)))
;; ```
;;
;;; (files (open-jar "path/to/jar"))
(defn files
  "Returns a sorted-set of files in the jar (that is: not directories)
   args: [jar]
   returns: sorted-set of string filepath (jar relative)"
  [jar]
  (paths-matching jar #(not (re-find #"/$" %))))

;; ### slurp-file
;; Read the entire contents of a given file within a Jar file into memory,
;; either as a utf-8 decoded string or as a ByteBuffer
;;
;; ```clojure
;; (defn get-fooclj [jar byteBuffer?]
;;    (slurp-file jar "foo.clj" byteBuffer?))
;; ```
;;
;;; (slurp-file (open-jar "path/to/jar") "file/name" false)
(defn slurp-file
  "Given a filename in the jar, slurps it
   args: [jar name & [binary?]]
   returns: if binary?, ByteBuffer, else string utf-8 decoded"
  [^JarFile jar ^String name & binary?]
  (when-let [^JarEntry je (.getJarEntry jar name)]
    (let [s (.getInputStream jar je)
          size (.getSize je)
          bytes (byte-array size)]
      (.read s bytes 0 size)
      (if binary?
        (to-byte-buffer bytes)
        (String. bytes (java.nio.charset.Charset/forName "UTF-8"))))))

;; ### with-jar
;; Opens a given jar and locally bind it for operating on, returning
;; the result of the last expression.
;;
;; ```clojure
;; (println (with-jar jar "path/to/jar"
;;            (get-fooclj jar false)))        ; See slurp-file example
;; ```
;;; (with-jar jar "path/to/jar" (println (slurp-file jar "file/name") false))
(defmacro with-jar
  "Opens a jar, binding it locally to name and executes exprs
   args: [name path & exprs]
     name: symbol
     path: string path to jar
     exprs: expressions to run with name bound to the jar
   returns: result of last expr"
  [name path & exprs]
  `(let [~name (open-jar ~path)]
     (try
       ~@exprs
       (finally
         (close-jar ~name)))))

;; ### slurp-jar-matching
;; A quick convenince function that opens the specified Jar path, and returns
;; a map of `{filename filecontents}` for all file names in the Jar that match
;; a predicate.
;;
;; ```clojure
;; ;(slurp-jar-matching path predicatefunction binary?)
;; (map (fn [[filename contents]]
;;          (println (str "===[" filename "]==="))
;;          (println contents))
;;      (slurp-jar-matching "path/to/jar" #(re-find #".clj$") false))
;; ```
;;
;;; (slurp-jar-matching "path/to/jar" #(re-find #".clj$") false)
(defn slurp-jar-matching
  "Opens a jar at the given path, filters filenames by predicate and reads them,
   closing the jar afterwards. returns data as map of filename to content
   args: [jar-path pred binary?]
     pred: boolean predicate
     binary?: should we return a bytebuffer (true) or a string (false)?
   returns map of filename to content"
  [jar-path predicate binary?]
  (with-jar jar jar-path
    (let [fs (into [] (filter predicate) (entries jar))]
      (into {} (map (fn [f] [f (slurp-file jar f)])) fs))))

;; ### normalise-class
;; Convert a relative string path name into an equivalent java classname
;;
;; ```clojure
;; (= (normalise-class "path/to/foo.class") "path.to.foo")
;; ```
;;; (normalise-class "path/to/foo.class")
(defn normalise-class
  "Given the jar-relative path of a class, turns it into a java classname
   args: [path]
   returns: string"
  [path]
  (-> path
       (s/replace #"\.class$" "")
       (s/replace #"/" ".")))

;; ### normalise-namespace
;; Convert a string path to a clojure file into a respective namespace name
;;
;; ```clojure
;; (= (normalise-namespace "path/to/foo.clj") "path.to.foo")
;; ```
;;; (normalise-namespace "path/to/foo.clj")
(defn normalise-namespace
  "Given the jar-relative path of a clojure file, turns it into a namespace name
   args: [path]
   returns: string"
  [path]
  (-> path
       (s/replace #"\.clj[cs]?$" "")
       (s/replace #"/" ".")
       demunge))

;; ### clojure-in-jar
;; Return a sequence of paths in the Jar file which have clojure(script)
;; extensions.
;;
;; ```clojure
;; (map println (with-jar myjar "path/to/jar"
;;                  (clojure-in-jar myjar)))
;; ```
;;; (clojure-in-jar (open-jar "path/to/jar"))
(defn clojure-in-jar
  "Returns a sequence of file paths in the jar that look like clojure(script) files
   args: [jar]
   returns: seq of string"
  [jar]
  (paths-matching jar (partial re-find #"\.clj[sc]?$")))

;; ### classes-in-jar
;; Return a sequence of paths in the Jar file which have .class extensions.
;;
;; ```clojure
;; (map println (with-jar myjar "path/to/jar"
;;                  (classes-in-jar myjar)))
;; ```
;;; (classes-in-jar (open-jar "path/to/jar"))
(defn classes-in-jar
  "Returns a sequence of file paths in the jar that look like java classes
   args: [jar]
   returns: seq of string"
  [jar]
  (paths-matching jar (partial re-find #"\.class$")))
