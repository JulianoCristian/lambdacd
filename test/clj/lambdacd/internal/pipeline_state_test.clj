(ns lambdacd.internal.pipeline-state-test
  (:use [lambdacd.testsupport.test-util])
  (:require [clojure.test :refer :all]
            [lambdacd.internal.pipeline-state :refer :all]
            [lambdacd.util :as utils]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [lambdacd.testsupport.test-util :as tu]
            [clojure.data :as d]
            [clojure.java.io :as io]))

(defn- after-update [build id newstate]
  (let [state (atom clean-pipeline-state)]
    (update { :build-number build :step-id id :_pipeline-state state} newstate)
    @state))

(defn- after-running [build id]
  (let [state (atom clean-pipeline-state)]
    (running { :build-number build :step-id id :_pipeline-state state})
    @state))

(deftest pipeline-state-test
  (testing "that the next buildnumber is the highest build-number currently in the pipeline-state"
    (is (= 5 (next-build-number {:_pipeline-state (atom { 3 {} 4 {} 1 {}})})))
    (is (= 1 (next-build-number {:_pipeline-state (atom clean-pipeline-state)}))))
  (testing "that after notifying about running, the pipeline state will reflect this"
    (is (= { 42 { [0] { :status :running }}} (tu/without-ts (after-running 42 [0])))))
  (testing "that a new pipeline-state will be set on update"
    (is (= { 10 { [0] { :foo :bar }}} (tu/without-ts (after-update 10 [0] {:foo :bar})))))
  (testing "that update will not loose keys that are not in the new map" ; e.g. to make sure values that are sent on the result-channel are not lost if they don't appear in the final result-map
    (is (= { 10 { [0] { :foo :bar :bar :baz }}}
           (let [state (atom clean-pipeline-state)]
             (update { :build-number 10 :step-id [0] :_pipeline-state state} {:foo :bar})
             (update { :build-number 10 :step-id [0] :_pipeline-state state} {:bar :baz})
             (tu/without-ts @state)))))
  (testing "that update will set a first-updated-at most-recent-update-at timestamp"
    (let [first-update-timestamp (t/minus (t/now) (t/seconds 1))
          state (atom clean-pipeline-state)
          ctx { :build-number 10 :step-id [0] :_pipeline-state state}]
      (t/do-at first-update-timestamp (update ctx {:foo :bar}))
      (is (= {10 {[0] {:foo :bar :most-recent-update-at first-update-timestamp }}} @state))))
  (testing "that updating will save the current state to the file-system"
    (let [home-dir (utils/create-temp-dir)
          config { :home-dir home-dir }
          step-result { :foo :bar }
          ctx { :build-number 10  :step-id [0] :config config :_pipeline-state (atom nil)}]
      (t/do-at (t/epoch) (update ctx step-result))
      (is (= [{ "step-id" "0" "step-result" { "foo" "bar" "most-recent-update-at" "1970-01-01T00:00:00.000Z"}}] (json/read-str (slurp (str home-dir "/build-10/pipeline-state.json"))))))))

(defn- write-pipeline-state [home-dir build-number state]
  (let [dir (str home-dir "/" "build-" build-number)
        path (str dir "/pipeline-state.json")]
    (.mkdirs (io/file dir))
    (utils/write-as-json path state)))

(deftest initial-pipeline-state-test
  (testing "that with a clean home-directory, the initial pipeline-state is clean as well"
    (let [home-dir (utils/create-temp-dir)
          config {:home-dir home-dir}]
      (is (= clean-pipeline-state (initial-pipeline-state config)))))
  (testing "that it reads the state from disk correctly"
    (let [home-dir (utils/create-temp-dir)
          config {:home-dir home-dir}]
      (write-pipeline-state home-dir 1 [{:step-id "0" :step-result {:foo "bar" }}])
      (write-pipeline-state home-dir 2 [{:step-id "1-2" :step-result {:bar "baz" }}])
      (is (= {1 { [0]   {:foo "bar"}}
              2 { [1 2] {:bar "baz"}}} (initial-pipeline-state config))))))

(deftest notify-when-most-recent-build-running-test
  (testing "that we are being notified when the first step of the pipeline is finished"
    (let [pipeline-state (atom {0 {}})
          call-counter (atom 0)
          callback (fn [& _] (swap! call-counter inc))]
      (notify-when-most-recent-build-running { :_pipeline-state pipeline-state} callback)
      (is (= 0 @call-counter))
      (update {:step-id [1] :_pipeline-state pipeline-state  :build-number 0 } {:status :success})
      (is (= 1 @call-counter))
      (update {:step-id [2] :_pipeline-state pipeline-state  :build-number 0 } {:status :success})
      (is (= 1 @call-counter))
      (update {:step-id [1] :_pipeline-state pipeline-state  :build-number 1 } {:status :waiting})
      (is (= 1 @call-counter))
      (update {:step-id [1] :_pipeline-state pipeline-state  :build-number 1 } {:status :running})
      (is (= 1 @call-counter))
      (update {:step-id [1] :_pipeline-state pipeline-state  :build-number 1 } {:status :failure})
      (is (= 2 @call-counter))
      (update {:step-id [1] :_pipeline-state pipeline-state  :build-number 1 } {:status :failure})
      (is (= 2 @call-counter))
      (update {:step-id [2] :_pipeline-state pipeline-state  :build-number 2 } {:status :ok :retrigger-mock-for-build-number 1 })
      (is (= 2 @call-counter))
      (update {:step-id [2] :_pipeline-state pipeline-state  :build-number 3 } {:status :ok })
      (is (= 2 @call-counter)))))
