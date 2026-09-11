(ns app-diskutil.model-test
  (:require [app-diskutil.model :as model]
            [app-diskutil.page :as page]
            [app-diskutil.source :as source]
            [clojure.test :refer [deftest is testing]]
            [design-quality.audit :as dq]
            [mokuroku.catalog :as catalog]
            [mokuroku.item :as item]))

(def entries
  [{:uuid "u-boot" :name "Macintosh HD" :filesystem "APFS" :mount-point "/"
    :capacity 1000000000000 :used 950000000000 :internal? true}
   {:uuid "u-ext" :name "Backup" :filesystem "HFS+" :mount-point "/Volumes/Backup"
    :capacity 2000000000000 :used 400000000000 :removable? true}
   ;; capacity unreadable
   {:uuid "u-odd" :name "Weird" :filesystem "exFAT" :mount-point "/Volumes/Weird"}])

(defn- cat-of [es]
  (catalog/refresh (catalog/catalog (source/fixture-source "Volumes" es)
                                    model/default-query)))

(deftest free-space-is-never-negative
  ;; Providers disagree about reserved blocks, and some report used >
  ;; capacity on a full APFS container. A negative free space is never true
  ;; and always looks like a bug in the app rather than the provider.
  (is (= 50000000000 (model/free-space {:capacity 1000000000000 :used 950000000000})))
  (is (zero? (model/free-space {:capacity 100 :used 140})))
  (is (nil? (model/free-space {:capacity nil :used 5})) "unknown stays unknown"))

(deftest unknown-capacity-is-not-zero-percent
  ;; Showing 0% would put an unreadable volume at the safe end of a sorted
  ;; list, which is the opposite of a warning.
  (is (nil? (model/percent-used {:name "Weird"})))
  (is (nil? (model/percent-used {:capacity 0 :used 0})) "no division by zero")
  (is (= 95.0 (model/percent-used {:capacity 1000000000000 :used 950000000000}))))

(deftest identity-is-the-uuid-not-the-mount-point
  ;; The same disk mounts at /Volumes/Untitled 1 on the second plug-in. An id
  ;; that moves means Eject can target the wrong device.
  (let [a (model/entry->item {:uuid "u-ext" :name "Backup" :mount-point "/Volumes/Backup"})
        b (model/entry->item {:uuid "u-ext" :name "Backup" :mount-point "/Volumes/Backup 1"})]
    (is (= (:item/id a) (:item/id b)))
    (is (not= (item/attr a :mount-point) (item/attr b :mount-point)))))

(deftest fullest-first-and-unknowns-last
  (let [ids (mapv :item/id (:result/items (catalog/result (cat-of entries))))]
    (is (= "u-boot" (first ids)) "95% used, the reason the app was opened")
    (is (= "u-odd" (last ids)) "unknown capacity sorts last, not first")))

(deftest nearly-full-is-one-threshold
  (let [by-id (into {} (map (juxt :item/id identity)) (model/listing->items entries))]
    (is (model/nearly-full? (by-id "u-boot")))
    (is (not (model/nearly-full? (by-id "u-ext"))))
    (is (nil? (model/nearly-full? (by-id "u-odd")))
        "unknown is neither full nor safe, and says so")))

(deftest erase-is-not-offered
  ;; A button that either does nothing or does the worst possible thing.
  (let [c (catalog/select (cat-of entries) "u-ext")]
    (is (= #{:eject :copy-path}
           (set (map :command/id (:view/commands (catalog/view c))))))
    (is (= :unknown-command (:proposal/refused (catalog/propose c :erase))))
    (testing "eject is destructive and single-target"
      (let [p (catalog/propose c :eject)]
        (is (true? (:proposal/destructive? p)))
        (is (true? (:proposal/requires-confirmation? p))))
      (is (= :wrong-arity (:proposal/refused
                           (catalog/propose (catalog/select-all c) :eject)))
          "ejecting every volume at once is not a thing this app can be asked for"))))

(deftest reading-fullness-does-not-require-reading-contents
  (is (= "system/metrics" (:source/capability (model/descriptor))))
  (is (source/suspicious-empty? (source/granted [])))
  (is (source/denied? source/denied)))

(deftest window-meets-the-design-quality-floor
  (let [pages {"volumes" (page/render (cat-of entries))
               "selection" (page/render (catalog/select (cat-of entries) "u-ext"))
               "awaiting-grant" (page/render
                                 (catalog/catalog (source/fixture-source "Volumes" [])
                                                  model/default-query))}
        {:keys [overall pages] :as report} (dq/audit pages {:extra-axes dq/extra-axes})]
    (println "design-quality: aggregate" overall)
    (is (>= overall 98.0) (pr-str (:findings report)))
    (doseq [[nm r] pages] (is (>= (:overall r) 98.0) nm))))
