(defproject cljagents "0.1.0"
  :description "`cljagents` is a small library for interfacing with the 
  [Quagents](http://www.cs.rochester.edu/trac/quagents)
  project from The University of Rochester.
  

  Look for the code (and more details) on 
  [github](https://github.com/jcsims/cljagents)."
  :url "http://github.com/jcsims/cljagents"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/data.priority-map "0.0.5"]
                 [org.clojure/core.logic "0.8.7"]]
  :source-paths ["src/clj"]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/tools.trace "0.7.8"]]}})
