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

(ns org.soulspace.annuity.application.pdf-report
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [org.soulspace.xml.dsl.fo-dsl :as fo]
            [org.soulspace.xml.dsl.svg-dsl :as svg]
            [org.soulspace.xml.util :as xutil]
            [org.soulspace.cmp.fop :as fop]
            [org.soulspace.annuity.domain.annuity :as domain]
            [org.soulspace.annuity.application.annuity :as app]))

;;;;
;;;; PDF Report Generation using the XSL-FO DSL from 'xml.dsl'
;;;;   and Apache FOP via 'cmp.fop'
;;;;

(defn rp-spec
  [spec]
  (fo/table {:space-before "10pt"}
    (fo/table-column {:column-number "1"})
    (fo/table-column {:column-number "2"})
    (fo/table-column {:column-number "3"})
    (fo/table-column {:column-number "4"})
    (fo/table-body
      {}
      (fo/table-row
        {:border-style "none"}
        (fo/table-cell {:text-align "left"} (fo/block {} (app/i18n "label.amount")))
        (fo/table-cell {:text-align "left"} (fo/block {} (app/formatted-string app/money-fmt (domain/financial-rounder (:credit spec)))))
        (fo/table-cell {:text-align "left"} (fo/block {} (app/i18n "label.annuity")))
        (fo/table-cell {:text-align "left"} (fo/block {} (app/formatted-string app/money-fmt (domain/financial-rounder (:rate spec))))))
      (fo/table-row
        {:border-style "none"}
        (fo/table-cell {:text-align "left"} (fo/block {} (app/i18n "label.interestPercentage")))
        (fo/table-cell {:text-align "left"} (fo/block {} (str (domain/financial-rounder (:p-interest spec)))))
        (fo/table-cell {:text-align "left"} (fo/block {} (app/i18n "label.redemptionPercentage")))
        (fo/table-cell {:text-align "left"} (fo/block {} (str (domain/financial-rounder (:p-redemption spec))))))
      (fo/table-row
        {:border-style "none"}
        (fo/table-cell {:text-align "left"} (fo/block {} (app/i18n "label.terms")))
        (fo/table-cell {:text-align "left"} (fo/block {} (str (:term spec))))
        (fo/table-cell {:text-align "left"} (fo/block {} (app/i18n "label.redemptionPeriod")))
        (fo/table-cell {:text-align "left"} (fo/block {} (str (:payment-period spec))))))))

(defn rp-period
  [period]
  (fo/table-row
    {:border-style "solid"}
    (fo/table-cell {:text-align "right"} (fo/block {} (str (domain/financial-rounder (:period period)))))
    (fo/table-cell {:text-align "right"} (fo/block {} (str (domain/financial-rounder (:year period)))))
    (fo/table-cell {:text-align "right"} (fo/block {} (app/formatted-string app/money-fmt (domain/financial-rounder (:amount period)))))
    (fo/table-cell {:text-align "right"} (fo/block {} (app/formatted-string app/money-fmt (domain/financial-rounder (:rate period)))))
    (fo/table-cell {:text-align "right"} (fo/block {} (app/formatted-string app/money-fmt (domain/financial-rounder (:interest period)))))
    (fo/table-cell {:text-align "right"} (fo/block {} (app/formatted-string app/money-fmt (domain/financial-rounder (:redemption period)))))
    (fo/table-cell {:text-align "right"} (fo/block {} (app/formatted-string app/money-fmt (domain/financial-rounder (:c-interest period)))))
    (fo/table-cell {:text-align "right"} (fo/block {} (app/formatted-string app/money-fmt (domain/financial-rounder (:c-cost period)))))))

(defn rp-period-table
  [periods]
  (fo/table {:space-before "10pt"}
    (fo/table-column {:column-number "1"})
    (fo/table-column {:column-number "2"})
    (fo/table-column {:column-number "3"})
    (fo/table-column {:column-number "4"})
    (fo/table-column {:column-number "5"})
    (fo/table-column {:column-number "6"})
    (fo/table-column {:column-number "7"})
    (fo/table-column {:column-number "8"})
    (fo/table-header
      {}
      (fo/table-row
        {:border-style "solid" :background-color "lightgrey"}
        (fo/table-cell {:text-align "center"} (fo/block {} (app/i18n "label.period")))
        (fo/table-cell {:text-align "center"} (fo/block {} (app/i18n "label.year")))
        (fo/table-cell {:text-align "center"} (fo/block {} (app/i18n "label.amountRemaining")))
        (fo/table-cell {:text-align "center"} (fo/block {} (app/i18n "label.rate")))
        (fo/table-cell {:text-align "center"} (fo/block {} (app/i18n "label.interest")))
        (fo/table-cell {:text-align "center"} (fo/block {} (app/i18n "label.redemption")))
        (fo/table-cell {:text-align "center"} (fo/block {} (app/i18n "label.c-interest")))
        (fo/table-cell {:text-align "center"} (fo/block {} (app/i18n "label.c-cost")))))
    (apply fo/table-body
      {}
      (map rp-period periods))))

(def svgx (xml/parse-str
           "<svg:svg width='300' height='200' xmlns:svg='http://www.w3.org/2000/svg'>
              <svg:g>
                <svg:circle cx='120' cy='90' style='fill: gray' r='80'></svg:circle>
              </svg:g>
            </svg:svg>"))

(def svgc (svg/svg {:width 300 :height 200}
                   (svg/g {}
                          (svg/circle {:cx 120 :cy 90 :r 80 :style "fill: gray"}))))

(defn rp-redemption-interest-chart
  []
  (let [;svg1 (parse-str (chart-svg-string (get-redemption-interest-chart) 1280 960))
        ;svg1 (xml/parse-str svgx)
        ifo1 (fo/block {} (fo/instream-foreign-object {} svgc))]
    (println ifo1)
    (println "Generate IFO" (xml/emit-str ifo1))
    (fo/block {:content-width "300px" :content-height "200px"}
              (fo/instream-foreign-object {} svgc))))

(defn cumulated-chart-svg
  []
  (fo/block {}
    (fo/external-graphic {:src "cumulated-chart.svg"})))

(defn rp-layout
  []
  (fo/layout-master-set
    {}
    (fo/simple-page-master
      {:master-name "single" :page-height "297mm" :page-width "210mm"}
      (fo/region-body {:margin "7mm"}))))

(defn rp-content
  [state]
  (fo/page-sequence
    {:master-reference "single"}
    (fo/flow
      {:flow-name "xsl-region-body" }
      (fo/block {:font-family "Times" :font-size "16pt" :font-variant "small-caps"} (app/i18n "label.data"))
      (rp-spec (:spec state))
      (rp-period-table (:periods state))
      ;(cumulated-chart-svg)
      (rp-redemption-interest-chart)
      )))

(defn rp-report
  [state]
  (fo/root {}
    (rp-layout)
    (rp-content state)))

(defn generate-pdf-report
  [state]
  (let [fo-report (rp-report state)
        fop-factory (fop/new-fop-factory "fop.xconf")]
;    (println "Generate Report")
;    (println  (xml/emit-str fo-report))
    (fop/fo-to-pdf fop-factory (xutil/string-input-source (xml/emit-str fo-report)) (io/as-file "report.pdf"))
    (println "Saved report")))
