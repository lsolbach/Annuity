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

(ns org.soulspace.annuity.application.annuity
  (:require [clojure.java.io :as io]
            [org.soulspace.clj.java.text :as text]
            [org.soulspace.clj.java.i18n :as i18n]
            [org.soulspace.cmp.svg.graphics2d :as g2d]
            [org.soulspace.cmp.jfreechart.chart :as jfchart]
            [org.soulspace.cmp.jfreechart.dataset :as jfdata]
            [org.soulspace.annuity.domain.annuity :as domain]))

;;;;
;;;; Application Logic
;;;;

;;
;; Internationalization
;;

(def i18n (partial i18n/bundle-lookup (i18n/bundle "resources")))

;;
;; Period labels
;;
(def redemption-periods
  "Redemption period labels."
  [(i18n "label.redemptionPeriod.annually")
   (i18n "label.redemptionPeriod.semiannually")
   (i18n "label.redemptionPeriod.quarterly")
   (i18n "label.redemptionPeriod.monthly")])


;;;
;;; Formatter Definitions
;;;

(def money-fmt "Format for money." (text/decimal-format "0.00" {}))
(def percent-fmt "Format for percent." (text/percent-format))

(defn formatted-string
  "Returns a formatted string representation of value 'v' using the formatter 'fmt'."
  [fmt v]
  (.format fmt v))

;;;
;;; Spec I/O
;;;

(defn save-spec 
  "Save the credit specification to a file."
  ([spec]
    (save-spec spec (io/as-file "spec.dat")))
  ([spec file]
    (let [spec-data (with-out-str (pr spec))]
      (spit (.getName file) spec-data))))

(defn load-spec
  "Load a credit specification from file."
  [file]
  (domain/update-spec (domain/calc-spec (load-file (.getName file)))))


;;;
;;; Charts based on JFreeChart with cmp.jfreechart
;;;

; create data for the chart
(defn period-amount [entry]
  [(:period entry) (:amount entry)])

(defn period-rate [entry]
  [(:period entry) (:rate entry)])

(defn period-redemption [entry]
  [(:period entry) (:redemption entry)])

(defn period-interest [entry]
  [(:period entry) (:interest entry)])

(defn period-c-interest [entry]
  [(:period entry) (:c-interest entry)])

(defn period-c-cost [entry]
  [(:period entry) (:c-cost entry)])

;;
;; Term Chart
;;

(defn term-chart-data [periods]
  (jfdata/xy-series-collection [(jfdata/xy-series (i18n "label.rate") (map period-rate periods))
                                (jfdata/xy-series (i18n "label.interest") (map period-interest periods))
                                (jfdata/xy-series (i18n "label.redemption") (map period-redemption periods))]))

(defn term-chart-update [data periods]
  (.removeAllSeries data)
  (.addSeries data (jfdata/xy-series (i18n "label.rate") (map period-rate periods)))
  (.addSeries data (jfdata/xy-series (i18n "label.interest") (map period-interest periods)))
  (.addSeries data (jfdata/xy-series (i18n "label.redemption") (map period-redemption periods))))

(defn term-chart []
  (let [data (term-chart-data @domain/periods)
        chart (jfchart/xy-line-chart (i18n "label.term.chart")
                                     (i18n "label.period") (i18n "label.currency") data :vertical)]
;    (set-renderer-properties chart {:shapesVisible true
;                                    :shapesFilled true})
    (defn get-term-chart [] chart)
    (add-watch domain/periods :term-chart-update (fn [_ _ _ new-periods] (term-chart-update data new-periods)))
    chart))

;;
;; Cumulated Chart
;;

(defn cumulated-chart-data [periods]
  (jfdata/xy-series-collection [(jfdata/xy-series (i18n "label.amountRemaining") (map period-amount periods))
                                (jfdata/xy-series (i18n "label.c-interest") (map period-c-interest periods))
                                (jfdata/xy-series (i18n "label.c-cost") (map period-c-cost periods))]))

(defn cumulated-chart-update [data periods]
  (.removeAllSeries data)
  (.addSeries data (jfdata/xy-series (i18n "label.amountRemaining") (map period-amount periods)))
  (.addSeries data (jfdata/xy-series (i18n "label.c-interest") (map period-c-interest periods)))
  (.addSeries data (jfdata/xy-series (i18n "label.c-cost") (map period-c-cost periods))))

(defn cumulated-chart []
  (let [data (cumulated-chart-data @domain/periods)
        chart (jfchart/xy-line-chart (i18n "label.cumulated.chart")
                                     (i18n "label.period") (i18n "label.currency") data :vertical)]
;    (set-renderer-properties chart {:shapesVisible true
;                                    :shapesFilled true})
    (defn get-cumulated-chart [] chart)
    (add-watch domain/periods :cumulated-chart-update (fn [_ _ _ new-periods] (cumulated-chart-update data new-periods)))
    chart))

;;
;; Redemption/Interest Chart
;;

(defn redemption-interest-chart-update [data periods]
  (let [c-interest (domain/calc-cumulated-interest periods)
        c-redemption (domain/calc-cumulated-redemption periods)]
    (.clear data)
    (.setValue data (i18n "label.c-redemption") (domain/financial-rounder c-redemption))
    (.setValue data (i18n "label.c-interest") (domain/financial-rounder c-interest))))

(defn redemption-interest-chart []
  (let [data (jfdata/pie-dataset [[(i18n "label.c-redemption") (domain/financial-rounder (domain/calc-cumulated-redemption @domain/periods))]
                                  [(i18n "label.c-interest") (domain/financial-rounder (domain/calc-cumulated-interest @domain/periods))]])
        chart (jfchart/pie-chart (i18n "label.redemptionInterest.chart") data true true true)]
    (defn get-redemption-interest-chart [] chart)
    (add-watch domain/periods :redemption-interest-chart-update (fn [_ _ _ new-periods] (redemption-interest-chart-update data new-periods)))
    chart))

;;
;; Chart Exports
;;
(defn chart-svg-string [chart width height]
  (g2d/svg-to-string (g2d/to-svg (partial jfchart/draw-chart-with-graphics2d chart (g2d/rectangle-2d width height)))))

(defn save-chart-as-svg [svg-file chart width height]
  (g2d/svg-to-file svg-file (g2d/to-svg (partial jfchart/draw-chart-with-graphics2d chart (g2d/rectangle-2d width height)))))

(defn save-charts []
  (when (seq @domain/periods)
    ; only save charts when data is available
    (save-chart-as-svg "term-chart.svg" (get-term-chart) 640 480)
    (save-chart-as-svg "cumulated-chart.svg" (get-cumulated-chart) 640 480)
    (save-chart-as-svg "redemption-interest-chart.svg" (get-redemption-interest-chart) 640 480)))
