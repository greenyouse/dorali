(ns dorali.es.core
  "Transitives for creating and composing SQL queries"
  (:require [deepfns.core :as deep]
            [deepfns.transitive :as t]))

(def query>
  "Runs a transitive and returns an ES query.

  ((query>
    {:query
     {:match-all (constantly {})}
     :size (es/default> 10 :size)})
   {:size 5})

  => {:query
      {:match-all {}}
      :size 5}"
  deep/transitive)


(defn percolate>
  "Returns an ES percolate query."
  ([field doc-type document]
   (t/when> (t/and> field doc-type document)
     {:field field :document-type doc-type :document document}))
  ([field doc-type index type id & {:keys [routing preference version]}]
   (t/when> (t/and> field doc-type index type id)
     {:field field
      :document-type doc-type
      :index index
      :type type
      :id id
      :routing routing
      :preference preference
      :version version})))

(defn template
  "Returns an ES template for the Search Template API."
  ([query params]
   (t/when> params
     {:query query :params params})))


;; intern transitives
(let [fs (ns-publics 'deepfns.transitive)]
  (doseq [[sym var] fs]
    (intern *ns* (with-meta sym (meta var)) var)))
