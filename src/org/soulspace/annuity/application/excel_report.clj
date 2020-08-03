;;
;;   Copyright (c) Ludger Solbach. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file license.txt at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;

(ns org.soulspace.annuity.application.excel-report
  (:use [org.soulspace.annuity.domain annuity]
        [org.soulspace.annuity.application i18n]
        [org.soulspace.cmp.poi.excel])
  (:import [org.apache.poi.ss.usermodel IndexedColors]))

;;
;; Excel Report Generation with cmp.poi based on Apache POI
;;

(defn generate-excel-report
  []
  (write-workbook
    "Report.xlsx"
    (new-workbook
      {}
      (let [spec @spec
            redemptions @redemptions
            periods @periods
            data-format (new-data-format {})
            heading-font (new-font {:fontHeightInPoints 16})
            heading-style (new-cell-style {:font heading-font})
            label-style (new-cell-style {:fillForegroundColor (color-index IndexedColors/LIGHT_YELLOW)
                                         :fillPattern (cell-fill-style :solid-foreground)})
            percent-style (new-cell-style {:alignment (horizontal-alignment :right) :dataFormat (.getFormat data-format "0.00%")})
            money-style (new-cell-style {:alignment (horizontal-alignment :right) :dataFormat (.getFormat data-format "0.00")})
            period-style (new-cell-style {:alignment (horizontal-alignment :right) :dataFormat (.getFormat data-format "0")})]
        
        (defn redemption-row
          [redemption]
          (new-row {}
                   (new-cell {:cellStyle period-style} (str (financial-rounder (:period redemption))))
                   (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:amount redemption))))))

        (defn period-row
          [period]
          (new-row {}
                   (new-cell {:cellStyle period-style} (str (financial-rounder (:period period))))
                   (new-cell {:cellStyle period-style} (str (financial-rounder (:year period))))
                   (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:amount period))))
                   (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:rate period))))
                   (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:interest period))))
                   (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:redemption period))))
                   (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:c-interest period))))
                   (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:c-cost period))))))

        ; Spec Sheet
        (new-sheet {}
                   (new-row {})
                   (new-row {}
                            (new-cell {:cellStyle heading-style} (i18n "label.data")))
                   (new-row {}
                            (new-cell {:cellStyle label-style} (i18n "label.amount"))
                            (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:credit spec)))))
                   (new-row {}
                            (new-cell {:cellStyle label-style} (i18n "label.annuity"))
                            (new-cell {:cellType (cell-type :numeric) :cellStyle money-style} (str (financial-rounder (:rate spec)))))
                   (new-row {}
                            (new-cell {:cellStyle label-style} (i18n "label.interestPercentage"))
                            (new-cell {:cellType (cell-type :numeric) :cellStyle percent-style} (str (financial-rounder (:p-interest spec)))))
                   (new-row {}
                            (new-cell {:cellStyle label-style} (i18n "label.redemptionPercentage"))
                            (new-cell {:cellType (cell-type :numeric) :cellStyle percent-style} (str (financial-rounder (:p-redemption spec)))))
                   (new-row {}
                            (new-cell {:cellStyle label-style} (i18n "label.terms"))
                            (new-cell {:cellStyle period-style} (str (financial-rounder (:term spec)))))
                   (new-row {}
                            (new-cell {:cellStyle label-style} (i18n "label.redemptionPeriod"))
                            (new-cell {:cellStyle period-style} (str (:payment-period spec)))))
      
        ; Extra Redemption Sheet
        (new-sheet {}
                   (new-row {})
                   (new-row {}
                            (new-cell {:cellStyle heading-style} (i18n "label.extraRedemptions")))
                   (new-row {}
                            (new-cell {:cellStyle label-style} (i18n "label.period"))
                            (new-cell {:cellStyle label-style} (i18n "label.redemption")))
                   (doseq [redemption redemptions]
                     (redemption-row redemption)))

        ; Redemption Plan Sheet
        (new-sheet {}
                   (new-row {})
                   (new-row {}
                            (new-cell {:cellStyle heading-style} (i18n "label.redemptionPlan")))
                   (new-row {}
                            (new-cell {:cellStyle label-style} (i18n "label.period"))
                            (new-cell {:cellStyle label-style} (i18n "label.year"))
                            (new-cell {:cellStyle label-style} (i18n "label.amountRemaining"))
                            (new-cell {:cellStyle label-style} (i18n "label.rate"))
                            (new-cell {:cellStyle label-style} (i18n "label.interest"))
                            (new-cell {:cellStyle label-style} (i18n "label.redemption"))
                            (new-cell {:cellStyle label-style} (i18n "label.c-interest"))
                            (new-cell {:cellStyle label-style} (i18n "label.c-cost")))
                   (doseq [period periods]
                     (period-row period)))))))
