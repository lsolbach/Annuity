;;;;
;;;;   Copyright (c) Ludger Solbach. All rights reserved.
;;;;
;;;;   The use and distribution terms for this software are covered by the
;;;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;;;   which can be found in the file license.txt at the root of this distribution.
;;;;   By using this software in any fashion, you are agreeing to be bound by
;;;;   the terms of this license.
;;;;
;;;;   You must not remove this notice, or any other, from this software.
;;;;

(ns org.soulspace.annuity.domain.annuity
  (:require [clojure.math :as m]
            [org.soulspace.math.interest :as mi]))

;;;;
;;;; Domain Logic for annuity credit calculations
;;;;

;;
;; Domain Data
;;

(defrecord AnnuitySpec [credit p-interest rate p-redemption term payment-period extra-redemptions])
(defrecord AnnuityPeriod [period year amount rate interest redemption c-interest c-cost])
(defrecord ExtraRedemption [period amount])

(defn new-spec
  ([] (AnnuitySpec. 0 0 0 0 0 0 []))
  ([credit p-interest rate p-redemption term payment-period]
    (AnnuitySpec. credit p-interest rate p-redemption term payment-period []))
  ([credit p-interest rate p-redemption term payment-period extra-redemptions]
    (AnnuitySpec. credit p-interest rate p-redemption term payment-period extra-redemptions)))

(defn new-period
  ([] (AnnuityPeriod. 0 0 0 0 0 0 0 0))
  ([period year amount rate interest redemption c-interest c-cost]
    (AnnuityPeriod. period year amount rate interest redemption c-interest c-cost)))

(defn new-redemption 
  ([] (ExtraRedemption. 0 0))
  ([period ammount]
    (ExtraRedemption. period ammount)))

;;
;; Domain State and Updates
;;

(def payments-per-year [1, 2, 4, 12])

;;
;; Domain Functions
;;

(defn financial-rounder
  "Returns a finacial rounding function."
  [value]
  ((partial mi/round-financial 2) value))

(defn years
  "Calculates the number of years it takes to pay back the credit."
  [periods payments-per-year]
  (m/ceil (/ periods payments-per-year)))

(defn calc-cumulated-cost
  "Calculates the cumulated cost for the given periods."
  [periods]
  (reduce + (map #(:rate %) periods)))

(defn calc-cumulated-interest
  "Calculates the cumulated interest for the given periods."
  [periods]
  (reduce + (map #(:interest %) periods)))

(defn calc-cumulated-redemption
  "Calculates the cumulated redemption for the given periods."
  [periods]
  (reduce + (map #(:redemption %) periods)))

(defn extra-redemption-for-period
  "Calculates the extra redemptions for the given period."
  [period extra]
  (reduce + (map #(:amount %) (filter #(= (:period %) period) extra))))

(defn calc-spec
  "Calculates a valid specification for the given input."
  [spec]
  (let [pp (:payment-period spec)
        py (nth payments-per-year pp)
        k-0 (:credit spec)
        q (m/pow (+ 1 (mi/percent (:p-interest spec))) (/ 1 py))
        a (max (:rate spec) (* (- q (m/pow 0.995 (/ 1 py))) k-0)) ; minimum redemption 0.5% 
        i (:p-interest spec)
        i-t (:p-redemption spec)
        n (* (:term spec) py)
        extra (:extra-redemptions spec)]
    (if (and (> k-0 0) (> q 1))
      (cond
        (> a 0) ; calculate with fixed annuity
        (new-spec k-0 i a i-t (m/ceil (/ (mi/annuity-term k-0 a q) py)) pp extra)
        (> n 0) ; calculate with fixed term
        (new-spec k-0 i (mi/annuity-rate k-0 q n) i-t (m/ceil (/ n py)) pp extra)
        ; (> i-t 0) () ; ?
        :else spec)
      spec)))

(defn calc-periods-for-spec
  "Calculates the payment periods for the given credit specification."
  [spec]
  (let [py (nth payments-per-year (:payment-period spec))
        k-0 (:credit spec)
        q (m/pow (+ 1 (mi/percent (:p-interest spec))) (/ 1 py))
        a (:rate spec)
        i (- q 1)
        extra (:extra-redemptions spec)]
    (defn calc-periods-new [m periods]
      (let [prev-period (last periods) ; previous period
            r-m (extra-redemption-for-period m extra) ; extra redemption for period
            a-m (+ a r-m)
            k-m (if-not prev-period k-0 (- (* (:amount prev-period) q) a-m))
            ;k-m (- (* (get prev-period :amount k-0) q) a-m) ; FIXME k0
            ;k-m (annuity-credit-Km k-0 a-m q m) ; FIXME extra redemtptions have to be handled
            i-m (* k-m i)
            a-act (min (+ k-m i-m) a-m)]
        (if (> k-m 0)
          (recur (inc m) (conj periods (new-period
                                         (inc m) (int (+ (m/floor (/ m py)) 1))
                                         k-m a-act i-m (- a-act i-m)
                                         (+ i-m (get prev-period :c-interest 0))
                                         (+ a-act (get prev-period :c-cost 0)))))
          periods)))
    (calc-periods-new 0 [])))
