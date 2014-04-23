(ns user
  (:require [clojure.tools.namespace.repl :refer (refresh)]
            [clojure.pprint :refer (pprint)]
            [clojure.tools.trace :as trace]
            [cljagents.pipes :as pipes]
            [cljagents.agent :as ag]
            [cljagents.prob1 :as prob1]
            [cljagents.prob2 :as prob2]
            [cljagents.prob3 :as prob3]
            [cljagents.prob4 :as prob4]
            [cljagents.prob5 :as prob5]
            [clojure.core.async :as async]
            [clojure.core.logic :as logic]
            [clojure.core.logic.arithmetic :as math]))

(def host "crunchbang.local")
(def port 6000)

(def system (atom {}))

(defn start [single-channel?]
  (reset! system (pipes/connect host port single-channel?)))

(defn stop []
  (reset! system (pipes/disconnect @system)))

(comment
  (start false)

  (ag/spawn-bot (:in @system) (:out @system))

  (stop)

  (prob5/assignment [96 -96] (:in @system) (:out @system))

  (ag/move-by (:in @system) 200 :heading 180)

  (ag/move (:in @system) :heading -90)

  (ag/location? (:in @system))

  (ag/pick-up (:in @system))

  (ag/check-inventory (:in @system))

  (ag/radar (:in @system) 200)

  (pipes/spawn-gold :x 96 :y -350)
  )
