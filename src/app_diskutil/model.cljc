(ns app-diskutil.model
  "Disk Utility's domain: volumes and what is on them.

  One capability, `system/metrics` — the same aggregate-only grant
  `app-monitor` uses for CPU and memory. Reading how full a disk is does not
  require the right to read what is on it, and this app never asks for
  `fs/browse`."
  (:require [mokuroku.item :as item]
            [mokuroku.source :as source]))

(def capability "system/metrics")

(def columns
  [(source/attribute :name "Name" :string)
   (source/attribute :used "Used" :bytes)
   (source/attribute :capacity "Capacity" :bytes)
   (source/attribute :percent-used "Used %" :percent)
   (source/attribute :filesystem "Format" :string)
   (source/attribute :mount-point "Mount Point" :string)])

(def commands
  "Eject is destructive and single-target. There is no Erase: this app can
  describe a volume, and a provider that could erase one has not been built.
  Offering it would produce a button that either does nothing or does the
  worst possible thing."
  #{:eject :copy-path})

(defn descriptor
  ([] (descriptor "Volumes"))
  ([scope]
   (source/descriptor
    {:id :app-diskutil/volumes
     :item-kind :volume
     :label scope
     :capability capability
     :commands commands
     :attributes columns})))

(defn free-space
  "Capacity minus used, floored at zero.

  Providers disagree about whether reserved blocks count as used, and some
  report used > capacity on a full APFS container. A negative free space is
  never true and always looks like a bug in the app rather than the provider."
  [{:keys [capacity used]}]
  (when (and (number? capacity) (number? used))
    (max 0 (- capacity used))))

(defn percent-used
  "Percentage of capacity in use, or nil when capacity is unknown.

  Not zero: a volume whose size could not be read is not an empty volume, and
  showing 0% would put it at the safe end of a sorted list."
  [{:keys [capacity used]}]
  (when (and (number? capacity) (number? used) (pos? capacity))
    (/ (Math/round (* 1000.0 (/ used capacity))) 10.0)))

(defn entry->item
  "The id is the volume UUID, not the mount point. Mount points move — the
  same disk mounts at /Volumes/Untitled 1 on the second plug-in — and an id
  that moves means Eject can target the wrong device."
  [{:keys [uuid name filesystem mount-point capacity used removable? internal?] :as v}]
  (item/item uuid
             :volume
             (or name mount-point uuid)
             {:name (or name mount-point uuid)
              :filesystem filesystem
              :mount-point mount-point
              :capacity capacity
              :used used
              :free (free-space v)
              :percent-used (percent-used v)
              :removable removable?
              :internal internal?}))

(defn listing->items [entries]
  (mapv entry->item entries))

(def fullest-first
  "The default: the volume about to run out at the top, which is the question
  Disk Utility is opened to answer."
  [[:percent-used :desc]])

(def default-query
  {:query/sort fullest-first :query/text "" :query/filters []})

(defn nearly-full?
  "Over 90% used. A threshold, stated once, rather than repeated as a literal
  in the view and the test."
  [it]
  (when-let [p (item/attr it :percent-used)]
    (> p 90.0)))
