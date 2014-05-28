(ns cljagents.prob2
  "Make a circuit around the room, making sure to keep against wall
  even around obstacles. This is desogned for the `maze1` map."
  (:use [cljagents.agent]
        [clojure.core.async :only [<!! >!!]]))

(def target (atom []))

(defmulti process-response
  "Process a response message from the engine"
  (fn [c r] (:op r)))

(defmethod process-response :lc
  [c r]
  ;; Check if we're close enough to the target location
  (let [loc (:data r)
        x (read-string (first loc))
        y (read-string (second loc))]
    (if (> (euclidean-distance [x y] @target) 20)
      (do
        (rangefinder c :distance 40 :theta -90)
        true)
      false)))

(defmethod process-response :rf
  [c r]
  (let [resp (:data r)
        dist (read-string (first resp))
        id (read-string (second resp))]
    (if (< id -1)
      (move-by c 20)
      (rotate c -90))))

(defmulti process-complete
  "Process a completion message from the engine."
  (fn [c r] (:op r)))

(defmethod process-complete :mb
  [c r]
  ;; If we've run into a wall, turn to the left
  (let [status (first (:data r))]
    (if (= status "blocked")
      (rotate c 90)
      (location? c)))
  true)

(defmethod process-complete :ro
  [c r]
  ;; rotation is completed, start moving again
  (move-by c 20)
  true)

(defmethod process-complete :default
  [c r]
  ;; Most of the time, we don't care about something when it's
  ;; completed. So, we do nothing
  true)

(defmulti process-message
  "The brains of the bot - respond to messages from the engine"
  (fn [[c r]] (:type r)))

(defmethod process-message :cp [[c r]] (process-complete c r))
(defmethod process-message :rs [[c r]] (process-response c r))


(defn assignment
  "Move along the exterior of the wall, keeping the wall along
  the bot's right side"
  [in-chan out-chan]
  (spawn-bot in-chan out-chan)
  ;; First get the initial location
  (move in-chan :heading 135)
  (<!! out-chan)
  (location? in-chan)
  (let [[_ msg] (<!! out-chan)
        loc (if (= :rs (:type msg))
              (:data msg)
              (do
                (>!! out-chan [in-chan msg]) ;; Toss the 'complete' message back
                (:data ((<!! out-chan) 1))))
        x (read-string (first loc))
        y (read-string (second loc))]
    (<!! out-chan) ;; Discard the "complete" message
    (reset! target [x y])
    (println (str ";; Making a circuit to x: " x " y: " y)))
  ;; Move away from the start point
  (rotate in-chan -90)
  (<!! out-chan)
  ;; Start the circuit
  (move-by in-chan 30)
  (while (process-message (<!! out-chan)))
  (println "Circuit completed"))
