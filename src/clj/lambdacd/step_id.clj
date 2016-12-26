(ns lambdacd.step-id
  (:require [lambdacd.util.internal.coll :as coll-util]))

(defn parent-of? [parent child]
  (let [cut-off-b (take-last (count parent) child)]
    (and
      (not= parent child)
      (= parent cut-off-b))))

(defn direct-parent-of? [parent child]
  (and
    (parent-of? parent child)
    (= (inc (count parent))
       (count child))))

(defn later-than? [a b]
  (let [length (max (count a) (count b))
        a-parents-first (reverse a)
        b-parents-first (reverse b)
        equal-length-a (coll-util/fill a-parents-first length -1)
        equal-length-b (coll-util/fill b-parents-first length -1)
        a-and-b (map vector equal-length-a equal-length-b)
        first-not-equal (first (drop-while (fn [[x y]] (= x y)) a-and-b))
        [x y] first-not-equal]
    (if (nil? first-not-equal)
      (> (count a) (count b))
      (> x y))))

(defn before? [a b]
  (and
    (not= a b)
    (not (later-than? a b))))

(defn child-id [parent-step-id child-number]
  (cons child-number parent-step-id))

(defn root-step-id? [step-id]
  (= 1 (count step-id)))

(defn root-step-id-of [step-id]
  (last step-id))
