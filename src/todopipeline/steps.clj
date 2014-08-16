(ns todopipeline.steps
  (:require [clojure.java.shell :as jsh]))

;; utilities
(defn mysh [cwd cmd]
  (jsh/sh "bash" "-c" cmd :dir cwd))

;; pipeline steps


(defn cwd [specified-working-directory]
  (fn [_]
    {:cwd specified-working-directory }))



;; TODO: this cannot handle error cases in the first two scripts
(defn client-package [{cwd :cwd}]
  (mysh cwd "bower install")
  (mysh cwd "./package.sh")
  (mysh cwd "./publish.sh"))


;; ----------------------------------------

(defn doCompile [a]
  (println "start compiling")
  (Thread/sleep 1000)
  (println "compiling done")
  {
   :artifacts ["build/*.jar"]})

(defn check [{artifacts :artifacts}]
  (println "Running unit tests")
  (Thread/sleep 2000)
  (println "Testresult: 10/10 succeeded")
  { :results
    { :testng
      { :successful 10
        :failed 0
        :running 10}}})

(defn jscheck [{artifacts :artifacts}]
  (println "Running Jasmine tests")
  (Thread/sleep 1000)
  (println "Testresult: 2 succeeded")
  { :results
    { :jasmine
      { :successful 10
        :failed 0}}})

(defn rsatobs [{artifacts :artifacts}]
  (println "Running Selenium Tests")
  (Thread/sleep 10000)
  (println "Testresult: 1 succeeded")
  { :results
    { :testng
      { :successful 1
        :failed 0
        :running 1}}})

(defn publishrpm [{artifacts :artifacts}]
  (println "Publishing artifacts")
;  (println (:out (sh "bash" "-c" "echo foo")))
  (Thread/sleep 1000)
  (println "Published artifact 1.75.19876")
  { :properties { :nexus-version "1.75.19876"}})

(defn deploy-ci [{{nexus-version :nexus-version} :properties}]
  (println (str "Deploying Nexus Version " nexus-version))
  (println "deployed"))