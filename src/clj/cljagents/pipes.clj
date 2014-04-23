(ns cljagents.pipes
  (:require [clojure.core.async
             :refer [<! >! <!! >!! go go-loop chan close! sliding-buffer]]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.net Socket SocketException)
           java.io.StringWriter))

(defn message->map
  [[response-type opcode idnum & data]]
  {:type (keyword response-type)
   :op (keyword opcode)
   :id (read-string idnum)
   :data data})

(defn parse-response
  "Parse the response from the quagent engine"
  [string-response]
  (let [response (string/split string-response #"\s")
        response-type (keyword (first response))]
    (if (= :id response-type)
      {:type :cp :op :id :id (read-string (response 1))}
      (message->map response))))

(defn- new-input-channel
  "Given a socket connection, return a channel that passes all input to the
  socket connection."
  [writer]
  (let [in (chan)]
    (go-loop [data (<! in)]
      (when data
        (println ";; Sending message: " data)
        (.write writer (str data "\n"))
        (.flush writer)
        (recur (<! in))))
    in))

(defn- send-off-msgs [out data mapped-response]
  (go (>! out { :op (:op mapped-response)
               :msgs (conj (data (:id mapped-response)) mapped-response)}))
  (dissoc data (:id mapped-response)))

(defn- new-combined-output-channel
  "Give a socket connection, return a channel that receives all output from
  the socket.

  This channel will combine all output with matching command id, and only send
  the combined messages when the 'cp' response has been recieved."
  [reader]
  (let [out (chan (sliding-buffer 10))]
    (go-loop [data {}
              response (.readLine reader)]
      (when response
        (println ";; Read message: " response)
        ;; Parse the response into a map
        (let [mapped-response (parse-response response)
              id (:id mapped-response)
              elems (data id)
              combined (conj elems mapped-response)
              data (if (= :cp (:type mapped-response))
                     (send-off-msgs out data mapped-response)
                     (assoc data id combined))]
          (recur data (.readLine reader)))))
    out))

(defn- new-single-output-channel
  "Give a socket connection, return a channel that receives all output from
   the socket. Puts each line into the channel as it comes."
  [in-channel reader]
  (let [out (chan (sliding-buffer 10))]
    (go-loop [data (.readLine reader)]
      (when data
        (println ";; Read message: " data)
        ;; Parse the response into a map
        (>! out [in-channel (parse-response data)])
        (recur (.readLine reader))))
    out))

(defn connect
  [host port single-channel?]
  (let [socket (Socket. host port)
        writer (io/writer socket)
        reader (io/reader socket)
        in (new-input-channel writer)
        out (if single-channel?
              (new-single-output-channel in reader)
              (new-combined-output-channel reader))]
    {:socket socket
     :writer writer
     :reader reader
     :in in
     :out out}))

(defn disconnect
  [{:keys [socket writer reader in out] :as system}]
  (println ";; Killing system:" system)
  (close! in)
  (close! out)
  (.close writer)
  (.close reader)
  {:socket socket
   :writer writer
   :reader reader
   :in nil
   :out nil})

(defn spawn-gold [& {:keys [x y z]
                     :or {x 880
                          y -880
                          z 88}}]
  (with-open [socket (Socket. "crunchbang.local" 6002)
              writer (io/writer socket)]
    (.write writer (str "dr 9999 1 20 " (string/join " " [x y z]) "\n"))
    (.flush writer)))
