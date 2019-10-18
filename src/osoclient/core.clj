(ns osoclient.core
  (:gen-class)
  (:require [osoclient.cache :as cache]
            [osoclient.client :as client]))

;; Some parameters for execution
(def upper-bound 100) ; Upper bound for random input generation
(def lot-size 100000) ; Number of requests to make when generating input

(defn generate-requests
  ([] (generate-requests lot-size))
  ([n] (let [randnums (take n (repeatedly #(rand-int upper-bound)))]
         (println (str "Generating " n " random requests"))
         (client/reset-stats n)
         (doseq [num randnums]
           (client/squareme num)))))

(defn -main
  "Generate random requests to test client-side cache"
  [& args]
  (reset! client/running true)
  (if (empty? args)
    (generate-requests)
    (generate-requests (Integer/parseInt (first args))))
  (while @client/running
    (Thread/sleep 500)))
