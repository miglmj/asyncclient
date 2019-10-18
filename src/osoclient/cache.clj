(ns osoclient.cache)

(def cache-hits (atom 0))
(def cache-misses (atom 0))
(def square-cache (atom {}))

(defn- valid?
  "Returns true if expiration has not passed, false otherwise"
  [expiration]
  (let [cur-time (System/currentTimeMillis)]
    (< (compare cur-time expiration) 1)))

(defn- contains-and-is-valid?
  "Returns true if entry exists and is unexpired in cache"
  [k]
  (and (contains? @square-cache k)
       (valid? (:expiration (get @square-cache k)))))

(defn cache-set
  ([k v ttlms]
   (let [cur-time (System/currentTimeMillis)]
     (swap! square-cache #(assoc % k {:value v
                                      :expiration (+ cur-time ttlms)}))))
  ([cache k v ttlms]
    (let [cur-time (System/currentTimeMillis)]
      (assoc cache k {:value v :expiration (+ cur-time ttlms)}))))

(defn cache-get
  [k]
  (if (contains-and-is-valid? k)
    (do
      (swap! cache-hits inc)
      (:value (get @square-cache k)))
    (do
      (swap! cache-misses inc)
      nil)))

(defn hit-rate
  []
  (float (/ @cache-hits (+ @cache-hits @cache-misses))))

(defn clear-stats
  []
  (reset! cache-misses 0)
  (reset! cache-hits 0))

(defn clear-cache
  []
  (reset! square-cache {}))
