(ns cljagents.prob4
  (:require [clojure.core.logic :as logic]))

(defn assignment
  "Takes a vector of integers, and returns a list of integers
  to fill a single gap in the sequence. Returns the empty list
  if there is no gap."
  [v]
  (let [sorted (sort v)
        complete (range (first sorted) (inc (last sorted)))]
    (logic/run* [q]
      (logic/membero q complete)
      (logic/distincto (conj sorted q)))))
