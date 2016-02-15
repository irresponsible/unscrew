(ns irresponsible.unscrew.util)

(defmacro safely [& exprs]
  `(try ~@exprs
        (catch Exception e#)))

(defmacro safely-or [or-v & exprs]
  `(try ~@exprs
        (catch Exception e# ~or-v)))

(defn mapassoc-kv-with [kf vf init coll]
  (reduce-kv (fn [acc k v]
               (assoc acc (kf k) (vf v))) init coll))

(defn stringify-map [m]
  (mapassoc-kv-with str str {} m))

