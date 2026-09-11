(ns app-diskutil.page
  (:require [app-diskutil.model :as model]
            [mokuroku.catalog :as catalog]
            [mokuroku-ui.core :as mui]))

(def view-opts
  {:columns [:name :used :capacity]
   :formatters {:used mui/human-bytes
                :capacity mui/human-bytes
                :free mui/human-bytes
                :percent-used mui/percent}
   :noun "volumes"
   :search-placeholder "Search volumes"
   :empty-title "No volumes"
   :empty-body "The metrics provider returned nothing, which should not happen on a booted machine."
   :badge (fn [it] (when (model/nearly-full? it) "Nearly full"))
   :title "Disk Utility"
   :description "Volumes, fullest first."})

(defn render [cat] (mui/->page (catalog/view cat) view-opts))
(defn render-html [cat] (mui/->html (catalog/view cat) view-opts))
