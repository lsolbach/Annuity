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

(ns org.soulspace.annuity.application.excel-report
  (:require [org.soulspace.annuity.domain.annuity :as domain]
            [org.soulspace.annuity.application.annuity :as app]
            [org.soulspace.cmp.poi.excel :as excel])
  (:import [org.apache.poi.ss.usermodel IndexedColors]))

;;;;
;;;; Excel Report Generation with cmp.poi based on Apache POI
;;;;

(defn generate-excel-report
  []
  (excel/write-workbook
    "Report.xlsx"
    (excel/new-workbook
      {}
      (let [spec @domain/spec
            redemptions @domain/redemptions
            periods @domain/periods
            data-format (excel/new-data-format {})
            heading-font (excel/new-font {:fontHeightInPoints 16})
            heading-style (excel/new-cell-style {:font heading-font})
            label-style (excel/new-cell-style {:fillForegroundColor (excel/color-index IndexedColors/LIGHT_YELLOW)
                                         :fillPattern (excel/cell-fill-style :solid-foreground)})
            percent-style (excel/new-cell-style {:alignment (excel/horizontal-alignment :right) :dataFormat (.getFormat data-format "0.00%")})
            money-style (excel/new-cell-style {:alignment (excel/horizontal-alignment :right) :dataFormat (.getFormat data-format "0.00")})
            period-style (excel/new-cell-style {:alignment (excel/horizontal-alignment :right) :dataFormat (.getFormat data-format "0")})]
        
        (defn redemption-row
          [redemption]
          (excel/new-row {}
                   (excel/new-cell {:cellStyle period-style} (str (domain/financial-rounder (:period redemption))))
                   (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:amount redemption))))))

        (defn period-row
          [period]
          (excel/new-row {}
                   (excel/new-cell {:cellStyle period-style} (str (domain/financial-rounder (:period period))))
                   (excel/new-cell {:cellStyle period-style} (str (domain/financial-rounder (:year period))))
                   (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:amount period))))
                   (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:rate period))))
                   (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:interest period))))
                   (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:redemption period))))
                   (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:c-interest period))))
                   (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:c-cost period))))))

        ; Spec Sheet
        (excel/new-sheet {}
                   (excel/new-row {})
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle heading-style} (app/i18n "label.data")))
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.amount"))
                            (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:credit spec)))))
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.annuity"))
                            (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle money-style} (str (domain/financial-rounder (:rate spec)))))
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.interestPercentage"))
                            (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle percent-style} (str (domain/financial-rounder (:p-interest spec)))))
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.redemptionPercentage"))
                            (excel/new-cell {:cellType (excel/cell-type :numeric) :cellStyle percent-style} (str (domain/financial-rounder (:p-redemption spec)))))
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.terms"))
                            (excel/new-cell {:cellStyle period-style} (str (domain/financial-rounder (:term spec)))))
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.redemptionPeriod"))
                            (excel/new-cell {:cellStyle period-style} (str (:payment-period spec)))))
      
        ; Extra Redemption Sheet
        (excel/new-sheet {}
                   (excel/new-row {})
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle heading-style} (app/i18n "label.extraRedemptions")))
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.period"))
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.redemption")))
                   (doseq [redemption redemptions]
                     (redemption-row redemption)))

        ; Redemption Plan Sheet
        (excel/new-sheet {}
                   (excel/new-row {})
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle heading-style} (app/i18n "label.redemptionPlan")))
                   (excel/new-row {}
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.period"))
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.year"))
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.amountRemaining"))
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.rate"))
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.interest"))
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.redemption"))
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.c-interest"))
                            (excel/new-cell {:cellStyle label-style} (app/i18n "label.c-cost")))
                   (doseq [period periods]
                     (period-row period)))))))
