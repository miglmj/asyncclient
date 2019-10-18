(ns osoclient.cache-test
  (:require [clojure.test :refer :all]
            [osoclient.cache :refer :all]))

(defn test-fixture
  [f]
  (cache-set 3 9 (+ 1000 (System/currentTimeMillis)))
  (cache-set 2 4 (- 1000 (System/currentTimeMillis)))
  (f))

(use-fixtures :once test-fixture)

(deftest valid?-check
  (testing "Expired time is invalid"
    (is (= false (valid? 1000))))
  (testing "Valid time is valid"
    (is (= true (valid? (+ 100 (System/currentTimeMillis)))))))

(deftest contains-and-is-valid?-test
  (testing "Unexpired value is valid"
    (is (= true (contains-and-is-valid? 3))))
  (testing "Expired value is invalid"
    (is (= false (contains-and-is-valid? 2)))))

(deftest cache-get-test
  (testing "Missing value returns nil"
    (is (nil? (cache-get 5))))
  (testing "Existing value is returned"
    (is (= 9 (cache-get 3))))
  (testing "Expired value returns nil"
    (is (nil? (cache-get 4)))))

(deftest cache-set-test
  (testing "Set a value"
    (is (contains? (cache-set {} 7 49 1000) 7))))
