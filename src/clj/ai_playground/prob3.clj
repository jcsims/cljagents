(ns ai-playground.prob3
  (:use [clojure.data.priority-map :only [priority-map]]
        [ai-playground.agent]
        [clojure.core.async :only [<!!]]))

(defn base-map
  "Create a map of edges with no walls."
  [x y w h]
  (let [w (dec w)
        h (dec h)]
    (conj
     ;; First, create the interior. Edges at the exterior are one-way
     (into {}
           (for [i (range w) j (range h)
                 :let [x0 (+ x i) y0 (+ y j) x1 (inc x0) y1 (inc y0)]]
             {[[x0 y0] [x1 y0]] 10
              [[x1 y0] [x1 y1]] 10
              [[x1 y1] [x0 y1]] 10
              [[x0 y1] [x0 y0]] 10
              [[x0 y0] [x1 y1]] 14
              [[x1 y0] [x0 y1]] 14
              [[x1 y1] [x0 y0]] 14
              [[x0 y1] [x1 y0]] 14}))
     ;; Then, finish the exterior so that edges are two-way
     (into {}
           (for [i (range w)
                 :let [x0 (+ x i) x1 (inc x0)]]
             {[[x0 h] [x1 h]] 10
              [[x1 0] [x0 0]] 10}))
     (into {}
           (for [i (range h)
                 :let [y0 (+ y i) y1 (inc y0)]]
             {[[w y1] [w y0]] 10
              [[0 y0] [0 y1]] 10})))))

(defn wall-edges
  "Given a single location, return a vector of edges to remove."
  [[x y]]
  (let [outwards [[[x y] [(dec x) (dec y)]]
                  [[x y] [(dec x)  y]]
                  [[x y] [(dec x) (inc y)]]
                  [[x y] [x (dec y)]]
                  [[x y] [x (inc y)]]
                  [[x y] [(inc x) (dec y)]]
                  [[x y] [(inc x) y]]
                  [[x y] [(inc x) (inc y)]]
                  ;; Also prevent cutting corners
                  [[x (inc y)] [(inc x) y]]
                  [[(inc x) y] [x (dec y)]]
                  [[x (dec y)] [(dec x) y]]
                  [[(dec x) y] [x (inc y)]]
                  ]]
    (into outwards (mapv #(vec (reverse %)) outwards))))

(defn remove-walls
  "Given a base map, remove edges that would lead to walls.
  Walls is a vector of vectors representing wall locations"
  [base-map walls]
  (apply dissoc base-map
         (reduce (fn [x y] (into x y))
                 []
                 (map wall-edges walls))))



(defn neighbors [edges]
  (reduce (fn [m [a b]] (assoc m a (conj (m a #{}) b)))
          {} (keys edges)))

;; Credit goes to Christophe Grande:
;; http://clj-me.cgrand.net/2010/09/04/a-in-clojure/
(defn A*
  "Finds a path between start and goal inside the graph described by edges
  (a map of edge to distance); estimate is an heuristic for the actual
  distance. Accepts a named option: :monotonic (default to true).
  Returns the path if found or nil."
  [edges heuristic start goal & {mono :monotonic :or {mono true}}]
  (let [f (memoize #(heuristic % goal))
        neighbors (neighbors edges)]
    (loop [q (priority-map start (f start))
           preds {}
           shortest {start 0}
           done #{}]
      (when-let [[x hx] (peek q)]
        (if (= goal x)
          (reverse (take-while identity (iterate preds goal)))
          (let [dx (- hx (f x))
                bn (for [n (remove done (neighbors x))
                         :let [hn (+ dx (edges [x n]) (f n))
                               sn (shortest n Double/POSITIVE_INFINITY)]
                         :when (< hn sn)]
                     [n hn])]
            (recur (into (pop q) bn)
                   (into preds (for [[n] bn] [n x]))
                   (into shortest bn)
                   (if mono (conj done x) done))))))))

(defn print-map
  "Given a map of edges and the dimensions of the overall map, pretty print it"
  [edges x y]
  (let [neighbors (neighbors edges)
        neighbor-counts (into {}
                              (for [i (range x) j (range y)]
                                {[i j] (neighbors [i j])}))]
    (print " ")
    (doseq [i (range x)]
      (printf "%5d" i))
    (newline)
    (printf " %2d" (dec y))
    (doseq [j (range (dec y) -1 -1) i (range x)]
      (print "[" (count (neighbor-counts [i j])) "]")
      (if (and (> j 0) (= i (dec x)))
        (printf "\n %2d" (dec j))))))

(def maze1
  "Create a map of edges representing the maze1 map of quagents"
  (remove-walls (base-map 0 0 19 19)
                [
                 ;; Long corridor on the left
                 [3 3] [3 4] [3 5] [3 6] [3 7] [3 8] [3 9]
                 [3 10] [3 11] [3 12] [3 13] [3 14] [3 15]

                 ;; L-shape on the bottom of the map
                 [7 3] [8 3] [9 3] [10 3] [11 3] [12 3]
                 [13 3] [14 3] [15 3]

                 [15 0] [15 1] [15 2]

                 ;; End of wrong path
                 [4 7] [5 7] [6 7] [7 7]

                 ;; Left side of s-curve
                 [7 8] [7 9] [7 10] [7 11] [7 12] [7 13]
                 [7 14] [7 15]

                 ;; Top of S
                 [8 15] [9 15] [10 15] [11 15] [12 15]
                 [13 15] [14 15]

                 ;; Left wall near goal
                 [15 11] [15 12] [15 13] [15 14]
                 [15 15] [15 16] [15 17] [15 18]

                 ;; Middle of S
                 [11 8] [11 9] [11 10] [11 11]

                 [11 7] [12 7] [13 7] [14 7] [15 7] [16 7] [17 7] [18 7]
                 ]))

(defn coords->engine
  "Given a pair of coordinates from the A* algorithm, convert it to
  a valid location in the maze1 level of the quagents engine."
  [[x y]]
  (let [x (+ 40 (* x 49))
        y (* -1 (+ 40 (* y 49)))]
    [x y]))

(defn assignment
  "Use the A* algorithm to find the optimal path from the spawn point to
  the goal."
  [in-chan out-chan]
  (spawn-bot in-chan out-chan)
  (doseq [[x y] (map coords->engine
                      (A* maze1 euclidean-distance [1 1] [17 17]))]
    (move-to in-chan x y)
    (<!! out-chan)))
