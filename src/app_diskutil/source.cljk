(ns app-diskutil.source
  "The `system/metrics` seam. Aggregate only; this app never lists files."
  (:require [app-diskutil.model :as model]
            [mokuroku.source :as source]))

(defrecord VolumeSource [scope read-fn]
  source/ISource
  (-descriptor [_] (model/descriptor scope))
  (-fetch [_] (model/listing->items (read-fn))))

(defn volume-source [scope read-fn] (->VolumeSource scope read-fn))
(defn fixture-source [scope entries] (volume-source scope (constantly entries)))

(def denied
  {:volumes/state :denied :volumes/capability model/capability :volumes/entries []})

(defn granted [entries]
  {:volumes/state :granted :volumes/capability model/capability
   :volumes/entries (vec entries)})

(defn denied? [r] (= :denied (:volumes/state r)))

;; A machine always has at least a boot volume, so an empty list is a provider
;; fault rather than a fact -- the same shape as an empty process table.
(defn suspicious-empty? [r]
  (and (not (denied? r)) (empty? (:volumes/entries r))))
