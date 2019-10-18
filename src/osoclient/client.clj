(ns osoclient.client
  (:require [osoclient.cache :as cache :refer [cache-get cache-set]]
            [osoclient.remote-api :as api]
            [clojure.core.async :as async :refer [chan >! alts!! go <! thread <!! >!!]]))


;; Execution parameters
(def worker-count 10) ; Number of processor threads to spawn
(def error-count (atom 0))

;; Execution state
(def running (atom false)) ; Used for waiting for all requests to be served before exiting
(def current-size (atom 0)) ; Number of requests received so far
(def counted (atom 0)) ; Number of requests handle
(def start-time (atom 0)) ; For timing execution

(defn reset-stats
  [n]
  (reset! start-time (System/currentTimeMillis))
  (reset! counted 0)
  (reset! current-size n)
  (cache/clear-stats)
  (cache/clear-cache))

(defn get-from-cache
  "Gets number from cache if valid"
  [number]
  (if-let [cached-value (cache-get number)]
    {:number number
     :square cached-value
     :cached true}))

(defn cache-checker
  "Checks in cache for any number received in input, then passes data to correct channel upon hit or miss"
  [input-channel hit-channel miss-channel]
  (go (while true
        (let [input (<! input-channel)]
          (if-let [output (get-from-cache input)]
            (>! hit-channel output)
            (>! miss-channel {:number input
                              :cached false}))))))

(defn response-handler
  "Writes success and error to appropriate channels."
  [success-channel error-channel]
  (fn [{:keys [status header body error opts]}]
    (let [{:keys [number]} opts]
      (if (and body
               (not (contains? (api/parse-response body) :error)))
        (let [parsed-body (api/parse-response body)]
          (thread (>!! success-channel {:number number
                                        :square (:msg parsed-body)
                                        :ttlms (:ttlms parsed-body)
                                        :cached false})))
        (do
          (swap! error-count inc)
          (thread (>!! error-channel number)))))))

(defn query-handler
  "Makes async request to API for any number received over input-channel."
  [input-channel success-channel error-channel]
  (thread (while true
            (let [input (<!! input-channel)]
              (api/request-square (:number input) (response-handler success-channel error-channel))))))

(defn cache-setter
  "Saves in cache anything received through input-channel, then passes data along to output-channel"
  [input-channel output-channel]
  (go (while true
        (let [{:keys [cached number square ttlms]} (<! input-channel)]
          (when-not cached
            (cache-set number square ttlms))
          (>! output-channel {:number number
                              :square square
                              :cached true})))))

(defn printer
  "Sends anything received over input to stdout, then passes it on"
  [input-channel output-channel]
  (go
    (while true
      (let [result (<! input-channel)
            {:keys [number square]} result]
        (println (str "The square of " number " is " square))
        (>! output-channel result)))))

(defn counter
  "Counts messages received over input channel. Once inputs received is equal to squares requested, prints statistics of execution."
  [input-channel]
  (go (while true
        (let [input (<! input-channel)]
          (if (= (- @current-size 1) @counted)
            (do
              (println (str "Squared " @current-size " numbers in " (- (System/currentTimeMillis) @start-time) "ms"))
              (println (str "Cache hit rate: " (format "%f" (cache/hit-rate))))
              (println (str "Error count: " @error-count))
              (reset! running false))
            (swap! counted inc))))))


;; Communication channels between threads/go blocks
(def input-queue (chan 1000000))
(def need-request-queue (chan 1000000))
(def need-caching-queue (chan 1000000))
(def output-queue (chan 1000000))
(def counting-queue (chan 1000000))

;; Initialize listeners
(cache-checker input-queue output-queue need-request-queue)
(doseq [_ (range worker-count)] ;; Initialize n workers
  (query-handler need-request-queue need-caching-queue input-queue)
  (cache-setter need-caching-queue output-queue))
(printer output-queue counting-queue)
(counter counting-queue)

(defn squareme
  [number]
  (do
    (>!! input-queue number)
    (Thread/sleep 3)))
