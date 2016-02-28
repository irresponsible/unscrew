(ns irresponsible.unscrew
  (:require [byte-streams :refer [to-byte-buffer]])
  (:import [java.util.jar JarFile JarEntry]))

(defn- mapassoc-kv-with [kf vf init coll]
  (reduce-kv (fn [acc k v]
               (assoc acc (kf k) (vf v))) init coll))

(defn- stringify-map [m]
  (mapassoc-kv-with str str {} m))

(defn open-jar
  "Opens jar file at given path
   args: [path]`
   returns: JarFile"
  [path]
  (JarFile. path))

(defn close-jar
  "Closes the given JarFile
   args: [jar]"
  [^JarFile jar]
  (.close jar))

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

(defn transform-entries
  "Returns a sorted-set of filepaths in the jar by applying a transducer to the
   sequence of entries
   args: [jar xf]
   returns: set of string"
  [^JarFile jar xf]
  (->> jar .entries enumeration-seq
       (into (sorted-set) xf)))
             
(defn entries
  "Returns a sorted-set of filepaths in the jar
   args: [jar]
   returns: set of string"
  [jar]
  (transform-entries jar (map get-name)))

(defn entries-matching
  "Returns a sorted-set of filepaths in the jar for which (pred %) returns truthy
   args: [jar pred]
   returns: set of string"
  [jar pred]
  (transform-entries jar (comp (map get-name) (filter pred) )))

(defn files
  "Returns a sorted-set of files in the jar (that is: not directories)
   args: [jar]
   returns: sorted-set of string filepath (jar relative)"
  [jar]
  (entries-matching jar #(not (re-find #"/$" %))))

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
