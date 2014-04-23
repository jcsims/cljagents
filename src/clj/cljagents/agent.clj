(ns cljagents.agent
  (:use [cljagents.pipes :as pipes]
        [clojure.core.async :only [<!! >!!]]
        [clojure.math.numeric-tower :as math]
        [clojure.string :only [join]]))

(def ^:private command-count (atom 1))

;;; Helpers
(defn- command-id []
  (swap! command-count inc))

;;; Basic actions
(defn move
  "Move the bot.

  Optional parameters
  :obstacles - 1 - hop over low obstacles
               0 - report blocked for any obstacles (default)
  :heading - float, 0 is straight ahead (default), 90 is strafe left
  :speed - float, [-1.0,1.0] 1.0 is full speed ahead (default), 0 is stopped
  :time - float, how long the bot should move"
  [c & {:keys [obstacles heading speed time]
        :or {obstacles 1
             heading 0
             speed 1}}]
  (let [id (command-id)]
    (if time
      (>!! c (str "n mf " (join " " [id obstacles heading speed time])))
      (>!! c (str "n mi " (join " " [id obstacles heading speed ]))))))

(defn move-by
  "Move the bot a specific distance

  distance - float, how far the bot should walk

  Optional parameters
  :obstacles - 1 - hop over low obstacles
               0 - report blocked for any obstacles (default)
  :heading - float, 0 is straight ahead (default), 90 is strafe left
  :speed - float, [-1.0,1.0] 1.0 is full speed ahead (default), 0 is stopped"
  [c distance & {:keys [obstacles heading speed]
                 :or {obstacles 0
                      heading 0
                      speed 1}}]
  (let [id (command-id)]
    (>!! c (str "n mb " (join " " [id obstacles heading speed distance])))))

(defn move-to
  "Move the bot to a specific x,y coordinate

  x - float, x coordinate to move to
  y - float, y coordinate to move to

  Optional parameters
  :obstacles - 1 - hop over low obstacles
               0 - report blocked for any obstacles (default)
  :speed - float, [-1.0,1.0] 1.0 is full speed ahead (default), 0 is stopped "
  [c x y & {:keys [obstacles speed]
            :or {obstacles 1
                 speed 1}}]
  (let [id (command-id)]
    (>!! c (str "n mt " (join " " [id obstacles speed x y])))))

(defn move-for
  "Move the bot for a specified period of time,
  or until it's blocked.

  time - float, time (in seconds) that the bot should move

  Optional parameters
  :heading - float, direction from facing to move
  :obstacles - 1 - hop over low obstacles
               0 - report blocked for any obstacles (default)
  :speed - float, [-1.0,1.0] 1.0 is full speed ahead (default), 0 is stopped"
  [c time & {:keys [heading obstacles speed]
             :or {heading 0
                  obstacles 1
                  speed 1}}]
  (let [id (command-id)]
    (>!! c (str "n mf " (join " " [id obstacles heading speed time])))))

(defn rotate
  "Rotate the bot in a specific direction (including up and down)

  yaw - float, number of degrees to turn to the left (negative to turn right)

  Optional parameters
  :pitch - float, number of degrees to look down from the current view
           (may be negative to look up)"
  [c yaw & {:keys [pitch]
            :or {pitch 0}}]
  (let [id (command-id)]
    (>!! c (str "n ro " (join " " [id yaw pitch])))))

(defn pick-up
  "Pick up any items of type IT_QUAGENTITEM currently inside the bot's
  bounding box"
  [c]
  (let [id (command-id)]
    (>!! c (str "n pu " id))))

;;; Basic queries
(defn location? "Queries the current location of the bot" [c]
  (let [id (command-id)]
    (>!! c (str "n lc " id))))

(defn facing? "Queries the current facing of the bot" [c]
  (let [id (command-id)]
    (>!! c (str "n fc " id))))

(defn can-see?
  "Determine if the bot has line-of-sight to the entity with id entityid"
  [c entityid]
  (let [id (command-id)]
    (>!! c (str "n cs " (join " " [id entityid])))))

(defn radar
  "Queries for any entities within 'range' of the bot's current location"
  [c range]
  (let [id (command-id)]
    (>!! c (str "n ra " (join " " [id range])))))

(defn what-is? "Query an entityid for it's type"
  [c entityid]
  (let [id (command-id)]
    (>!! c (str "n wi " (join " " [id entityid])))))

(defn rangefinder
  "Query for the nearest object at a certain angle from the bot

  Optional parameters
  :type - integer, specifies the type of beam used (defaults to type 5)
          0 - infinitely narrow beam, stopped by permanent surfaces that would
              stop the player (walls, floors, ceilings, moving platforms)
          1 - infinitely narrow beam, stopped by anything that would stop
              player movement (like type 0, plus other things like quagents)
          2 - infinitely narrow beam, stopped by anything that would block a shot
          3 - infinitely narrow beam, blocked by anything in the environment
          4 - a beam the width of a quagent, otherwise identical to type 0
          5 - a beam the width of a quagent, otherwise identical to type 1
          6 - a beam the width of a quagent, otherwise identical to type 2
          7 - a beam the width of a quagent, otherwise identical to type 3
  :distance - integer, maximum range of the beam. (default is 8000)
  :theta - float, degrees left of facing to shoot the beam (default is 0)
  :phi - float, degrees down from facing to shoot the beam (default is 0)"
  [c & {:keys [type distance theta phi]
        :or {type 5
             distance 8000
             theta 0
             phi 0}}]
  (let [id (command-id)]
    (>!! c
         (str "n rf " (join " " [id type distance theta phi])))))

(defn check-inventory
  "Get a listing of the bot's inventory"
  [c]
  (let [id (command-id)]
    (>!! c (str "n ci " id))))

;;; Manage the command queue
(defn pop-command
  "Deletes the next command to be executed from the command queue"
  [c]
  (let [id (command-id)]
    (>!! c (str "n po " id))))

(defn pause-command
  "Pauses the quagent forever"
  [c]
  (let [id (command-id)]
    (>!! c (str "n pa " id))))

(defn forget-all
  "Clears the command queue, making the bot forget all commands in its queue"
  [c]
  (let [id (command-id)]
    (>!! c (str "n fa " id))))

(defn forget-most
  "Forget all but the last command sent to the agent's queue"
  [c]
  (let [id (command-id)]
    (>!! c (str "n fm " id))))

(defn peek-queue
  "Peek at the agent's command queue.

  depth - int, -1 is this command, 0 is the command currently executing,
          1 is the queue and beyond"
  [c depth]
  (let [id (command-id)]
    (>!! c (str "n pk " (join " " [id depth])))))

;;; Higher-level commands
(defn spawn-bot
  "Takes a map of initial options, and returns the id of the bot
  that's spawned.

  Optional:
  :name - name of the bot (string)"
  [in-chan out-chan & {:keys [name] }]
  (if name
    (>!! in-chan (str "name " name)))
  (>!! in-chan "ready")
  (<!! out-chan))

(defn euclidean-distance "Determines the Euclidean distance between 2 points"
  [[x1 y1] [x2 y2]]
  (let [x-squared (math/expt (- x1 x2) 2)
        y-squared (math/expt (- y1 y2) 2)]
    (math/sqrt (+ x-squared y-squared))))
