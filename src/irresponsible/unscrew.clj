(ns irresponsible.unscrew
  (:require [irresponsible.unscrew.util :refer [stringify-map]])
  (:import  [java.lang Exception]
            [java.util.jar JarFile JarEntry]))

(defn open-jar
  "Opens jar file at given path
   args: [path]`
   returns: JarFile"
  [path]
  (JarFile. path))

(defn close-jar
  "Closes the given jar file
   args: [jar]"
  [^JarFile jar]
  (.close jar))

(defn get-manifest
  "Returns the manifest for the jar"
  [^JarFile jar]
  (when-let [m (.getManifest jar)]
    (when-let [as (.getMainAttributes m)]
      (when-let [es (.entrySet as)]
        (stringify-map (into {} es))))))

(defn get-entries
  "Returns a set of filenames in the jar
   args: [jar]
   returns: set of string"
  [^JarFile jar]
  (->> jar .entries enumeration-seq
       (into #{} (map #(.getName %)))))

(defn slurp-file
  "Given a filename in the jar, slurps it
   args: [jar name & [binary?]]
   returns: if binary?, bytebuffer, else string decoded from utf-8"
  [^JarFile jar ^String name & [binary?]]
  (when-let [^JarEntry je (.getJarEntry jar name)]
    (let [s (.getInputStream jar je)
          size (.getSize je)
          bytes (byte-array size)]
      (.read s bytes 0 size)
      (if binary?
        bytes
        (String. bytes (java.nio.charset.Charset/forName "UTF-8"))))))

(defmacro with-jar
  "Opens a jar, binding it locally to name and executes exprs
   args: [name path & exprs]
     name: symbol
     path: string path to jar
     exprs: expressions to run with name bound to the jar
   returns: result of last expr"
  [name path & exprs]
  `(let [~name (open-jar path)
         r (do ~@exprs)]
     (close-jar ~name)
     r))

(defn slurp-jar-matching
  "Opens a jar at the given path, filters filenames by predicate and reads them,
   closing the jar afterwards. returns data as map of filename to content
   args: [jar-path pred binary?]
     pred: boolean predicate
     binary?: should we return a bytebuffer (true) or a string (false)?
   returns map of filename to content"
  [jar-path predicate binary?]
  (with-jar jar jar-path
    (let [fs (into [] (filter predicate) (get-entries j))]
      (into {} (map (fn [f] [f (slurp-file j f)])) fs))))

