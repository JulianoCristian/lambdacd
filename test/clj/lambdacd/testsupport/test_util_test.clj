(ns lambdacd.testsupport.test-util-test
  (:require [clojure.test :refer :all]
            [lambdacd.testsupport.test-util :refer :all]
            [clojure.core.async :as async]))

(defn some-function-changing-an-atom [a]
  (reset! a "hello")
  (reset! a "world"))

(defn some-step-taking-50ms [arg & _]
  (Thread/sleep 50)
  {:foo :bar})


(deftest atom-history-test
  (testing "that we can record the history of an atom"
    (let [some-atom (atom "")]
      (is (= ["hello" "world"]
             (atom-history-for some-atom (some-function-changing-an-atom some-atom)))))))

(deftest history-for-test
  (testing "that we can record the history of an atom"
    (let [some-atom (atom "")
          history-atom (history-for-atom some-atom)]
      (some-function-changing-an-atom some-atom)
      (is (= ["hello" "world"]
             @history-atom)))))

(deftest timing-test
  (testing "that my-time more or less accurately measures the execution time of a step"
    (is (close? 10 50 (my-time (some-step-taking-50ms {}))))))

(deftest without-keys-test
  (testing "that we can get rid of key-value pairs in a nested map"
    (is (= {:a {:b {:foo :bar} :d {:bar :baz}}} (without-key {:a {:b {:c 1 :foo :bar} :d {:c 2 :bar :baz}}} :c)))))
