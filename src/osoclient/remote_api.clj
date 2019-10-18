(ns osoclient.remote-api
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn strip [coll chars]
  (apply str (filter #(not ((set chars) %)) coll)))

(defn parse-response
  [response]
  (if (nil? response)
    (println "NIL RESPONSE")
    (json/read-str response :key-fn #(-> %
                                        (strip "()")
                                        keyword))))

(defn request-square
  [number callback]
  (let [url (str "http://localhost:5000/squareme?num=" number)
        options {:timeout 500
                 :number number}
        handler #(callback (parse-response %))]
    (http/get url options callback)))
