(ns dorali.sql.core
  "Transitives for creating and composing SQL queries"
  (:refer-clojure :exclude [format])
  (:require [deepfns.core :as deep]
            [deepfns.transitive :as t]
            [honeysql.core :as sql]))

(defn call
  "Represents a SQL function call. Name should be a keyword."
  ([name]
   (constantly (sql/call name)))
  ([name & args]
   (constantly (apply sql/call name args))))

(defn raw [s]
  "Represents a raw SQL string"
  (constantly (sql/raw s)))

(defn param [name]
  "Represents a SQL parameter which can be filled in later"
  (constantly (sql/param name)))


(defn- eval-entries
  ([seed fs m]
   (not-empty
     (reduce (fn [acc f]
               (f acc m))
       seed fs))))

(defn- apply-entries
  ([seed fs]
   (fn [m & ms]
     (if (not-empty ms)
       (map (partial eval-entries seed fs) (cons m ms))
       (eval-entries seed fs m)))))

(defn- transitive-lst
  ([f]
   (fn [result m]
     (if-let [v (f m)]
       (conj result v)
       result))))

(defn- transitive-map
  ([k f]
   (fn [result m]
     (if-some [v (f m)]
       (assoc result k v)
       result))))

(defn query>
  "Takes a transitive `f` and uses that to walk a datastructure.
  Returns a SQL query formatted for Honey SQL.

  ((query>
    {:select [:emp_id :fname :lname]
     :from [:employee]
     :where (term> := :lname (=> :surname))})
   {:surname \"smith\"})

  => {:select [:emp_id :fname :lname],
      :from [:employee],
      :where [:= :lname \"smith\"]}"
  ([f]
   (cond
     (map? f) (apply-entries {}
                  (map (fn [[k v]]
                         (transitive-map k (transitive v)))
                    f))
     (seq? f) (apply-entries '()
                    (map (comp transitive-lst transitive) f))
     (vector? f) (apply-entries []
                      (map (comp transitive-lst transitive) f))
     (set? f) (apply-entries #{}
                   (map (comp transitive-lst transitive) f))
     (fn? f) f
     :else
     (constantly f)))
  ([f m]
   ((transitive f) m))
  ([f m & ms]
   (apply (transitive f) (cons m ms))))


(defn- reduce-terms [kvs]
  (let [pairs (partition 2 kvs)]
    (reduce (fn [acc [k v]]
              (let [out (deep/<=> v)]
                (conj acc (constantly k) out)))
      [] pairs)))

(defn term>
  ([v]
   (let [out (deep/<=> v)]
     (t/when> out
       [out])))
  ([op v]
   (let [out (deep/<=> v)]
     (t/when> out
       [(constantly op) out])))
  ([op k v]
   (let [out (deep/<=> v)]
     (t/when> out
       [(constantly op) (constantly k) out])))
  ([op k v & kvs]
   ;; apply fn to every other
   (let [kvs (cons k (cons v kvs))
         out (reduce-terms kvs)]
     (t/when> (not-empty out)
       (vec (cons (constantly op) out))))))

;; intern transitives + sql/format
(let [fs (assoc (ns-publics 'deepfns.transitive)
                'format (var sql/format))]
  (doseq [[sym var] fs]
    (intern *ns* (with-meta sym (meta var)) var)))
