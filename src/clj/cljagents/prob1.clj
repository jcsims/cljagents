(ns cljagents.prob1
  "Make a single circuit of the room. This is designed around the 
  `medroom` map."
  (:use [cljagents.agent]
        [clojure.core.async :only [<!! >!!]]))

(def target (atom []))


;; These series of multimethods answer the question:
;; "Do I keep going?"
(defmulti process-response
  "Process a response message from the engine"
  (fn [c r] (:op r)))

(defmethod process-response :lc
  [c r]
  ;; Check if we're close enough to the target location
  (println ";; Processing location response with message: " r)
  (let [loc (:data r)
        x (read-string (first loc))
        y (read-string (second loc))]
    (if (> (euclidean-distance [x y] @target) 20)
      (do
        (move-by c 20)
        true)
      false)))

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
  "Move to to a wall, then make a circuit of the room, stopping
  where the bot first hit the wall"
  [in-chan out-chan]
  (Thread/sleep 1000) ; For some reason the bot turns after spawning
  ;; First move to the opposite wall, and get the target location
  (spawn-bot in-chan out-chan)
  ;; the bot initially spawns in the corner
  (move-by in-chan 500 :heading 90)
  (<!! out-chan)
  (move in-chan)
  (<!! out-chan)
  (location? in-chan)
  (let [[_ msg] (<!! out-chan)
        loc (if (= :rs (:type msg))
              (:data msg)
              (do
                (>!! out-chan [in-chan msg]) ;; Toss the complete message back
                (:data ((<!! out-chan) 1))))
        x (read-string (first loc))
        y (read-string (second loc))]
    (<!! out-chan) ;; Discard the "complete" message
    (reset! target [x y])
    (println (str ";; Making a circuit to x: " x " y: " y)))
  ;; Make a first left and move to the wall
  (rotate in-chan 90)
  (move in-chan)
  ;; Get (and discard) the two responses
  (<!! out-chan)
  (<!! out-chan)
  (move-by in-chan 20)
  ;; Loop until process-message returns false, in which case we're done
  (while (process-message (<!! out-chan)))
  (println "Circuit completed"))
