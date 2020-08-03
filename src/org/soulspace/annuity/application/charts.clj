;;
;;   Copyright (c) Ludger Solbach. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file license.txt at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;

(ns org.soulspace.annuity.application.charts
  (:use 
    [org.soulspace.cmp.jfreechart chart dataset export]
    [org.soulspace.cmp.svg graphics2d]
    [org.soulspace.annuity.domain annuity]
    [org.soulspace.annuity.application i18n]))

;;
;; Charts based on JFreeChart with cmp.jfreechart
;;

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
  (xy-series-collection [(xy-series (i18n "label.rate") (map period-rate periods))
                         (xy-series (i18n "label.interest") (map period-interest periods))
                         (xy-series (i18n "label.redemption") (map period-redemption periods))]))

(defn term-chart-update [data periods]
  (.removeAllSeries data)
  (.addSeries data (xy-series (i18n "label.rate") (map period-rate periods)))
  (.addSeries data (xy-series (i18n "label.interest") (map period-interest periods)))
  (.addSeries data (xy-series (i18n "label.redemption") (map period-redemption periods))))

(defn term-chart []
  (let [data (term-chart-data @periods)
        chart (xy-line-chart (i18n "label.term.chart")
                             (i18n "label.period") (i18n "label.currency") data :vertical)]
;    (set-renderer-properties chart {:shapesVisible true
;                                    :shapesFilled true})
    (defn get-term-chart [] chart)
    (add-watch periods :term-chart-update (fn [_ _ _ new-periods] (term-chart-update data new-periods)))
    chart))

;;
;; Cumulated Chart
;;

(defn cumulated-chart-data [periods]
  (xy-series-collection [(xy-series (i18n "label.amountRemaining") (map period-amount periods))
                         (xy-series (i18n "label.c-interest") (map period-c-interest periods))
                         (xy-series (i18n "label.c-cost") (map period-c-cost periods))]))

(defn cumulated-chart-update [data periods]
  (.removeAllSeries data)
  (.addSeries data (xy-series (i18n "label.amountRemaining") (map period-amount periods)))
  (.addSeries data (xy-series (i18n "label.c-interest") (map period-c-interest periods)))
  (.addSeries data (xy-series (i18n "label.c-cost") (map period-c-cost periods))))

(defn cumulated-chart []
  (let [data (cumulated-chart-data @periods)
        chart (xy-line-chart (i18n "label.cumulated.chart")
                             (i18n "label.period") (i18n "label.currency") data :vertical)]
;    (set-renderer-properties chart {:shapesVisible true
;                                    :shapesFilled true})
    (defn get-cumulated-chart [] chart)
    (add-watch periods :cumulated-chart-update (fn [_ _ _ new-periods] (cumulated-chart-update data new-periods)))
    chart))

;;
;; Redemption/Interest Chart
;;

(defn redemption-interest-chart-update [data periods]
  (let [c-interest (calc-cumulated-interest periods)
        c-redemption (calc-cumulated-redemption periods)]
    (.clear data)
    (.setValue data (i18n "label.c-redemption") (financial-rounder c-redemption))
    (.setValue data (i18n "label.c-interest") (financial-rounder c-interest))))

(defn redemption-interest-chart []
  (let [data (pie-dataset [[(i18n "label.c-redemption") (financial-rounder (calc-cumulated-redemption @periods))]
                          [(i18n "label.c-interest") (financial-rounder (calc-cumulated-interest @periods))]])
        chart (pie-chart (i18n "label.redemptionInterest.chart") data true true true)]
    (defn get-redemption-interest-chart [] chart)
    (add-watch periods :redemption-interest-chart-update (fn [_ _ _ new-periods] (redemption-interest-chart-update data new-periods)))
    chart))

;;
;; Chart Exports
;;
(defn chart-svg-string [chart width height]
  (svg-to-string (to-svg (partial draw-chart-with-graphics2d chart (rectangle-2d width height)))))

(defn save-chart-as-svg [svg-file chart width height]
  (svg-to-file svg-file (to-svg (partial draw-chart-with-graphics2d chart (rectangle-2d width height)))))

(defn save-charts []
  (when (seq @periods)
    ; only save charts when data is available
    (save-chart-as-svg "term-chart.svg" (get-term-chart) 640 480)
    (save-chart-as-svg "cumulated-chart.svg" (get-cumulated-chart) 640 480 )
    (save-chart-as-svg "redemption-interest-chart.svg" (get-redemption-interest-chart) 640 480 )))
