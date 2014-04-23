(defproject ai-playground "0.1.0-SNAPSHOT"
  :description "Simple programmatic interface for an AI bot using Quagents"
  :url "http://github.com/jcsims/ai-playground"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/tools.trace "0.7.8"]
                 [org.clojure/data.priority-map "0.0.5"]
                 [org.clojure/core.logic "0.8.7"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/tools.trace "0.7.8"]]}})
