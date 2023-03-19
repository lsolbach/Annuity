;;
;;   Copyright (c) Ludger Solbach. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file license.txt at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;

(ns org.soulspace.annuity.ui.swing
  (:require [clojure.tools.swing-utils :refer [do-swing-and-wait]]
            [org.soulspace.clj.java.awt :as awt]
            [org.soulspace.clj.java.awt.event :as event]
            [org.soulspace.cmp.jfreechart.swing :as jfswing]
            [org.soulspace.annuity.domain.annuity :as domain]
            [org.soulspace.clj.java.swing.constants :as swc]
            [org.soulspace.annuity.application.annuity :as app])
  (:use 
        [org.soulspace.clj.java.swing swinglib]
        [org.soulspace.annuity.application pdf-report excel-report])
  (:import [javax.swing Action BorderFactory JFrame]))

;;
;; Swing UI of the annuity application with clj.swing
;;

(declare ui-frame)

(def heading-font (awt/font (awt/font-names :dialog) [(awt/font-styles :bold)] 14))

(defn redemption-table-model
  []
  (mapseq-table-model
    [{:label (app/i18n "label.period") :key :period :edit false}
     {:label (app/i18n "label.redemption") :key :amount :edit false :converter domain/financial-rounder}]
    domain/redemptions))

(defn period-table-model
  []
  (mapseq-table-model
    [{:label (app/i18n "label.period") :key :period :edit false}
     {:label (app/i18n "label.year") :key :year :edit false}
     {:label (app/i18n "label.amountRemaining") :key :amount :edit false :converter domain/financial-rounder}
     {:label (app/i18n "label.rate") :key :rate :edit false :converter domain/financial-rounder}
     {:label (app/i18n "label.interest") :key :interest :edit false :converter domain/financial-rounder}
     {:label (app/i18n "label.redemption") :key :redemption :edit false :converter domain/financial-rounder}
     {:label (app/i18n "label.c-interest") :key :c-interest :edit false :converter domain/financial-rounder}
     {:label (app/i18n "label.c-cost") :key :c-cost :edit false :converter domain/financial-rounder}]
    domain/periods))

(defn open
  "Loads the specification."
  [filename]
  (println (str "Loading " filename))
  (app/load-spec filename))

(defn save
  "Save the specification."
  [filename]
  (println (str "Saving " filename))
  (app/save-spec @domain/spec))

;;
;; Dialogs
;;

(let [field-period (integer-field {:columns 10})
      field-amount (formatted-text-field app/money-fmt {:columns 10 :value 0.0})
      button-ok (button {:text (app/i18n "button.ok")})
      button-cancel (button {:text (app/i18n "button.cancel")})
      d (dialog {:title (app/i18n "dialog.extra-redemption.title")}                  
                [(panel {:layout (mig-layout {:layoutConstraints "wrap 2"})}
                        [(label {:text (app/i18n "label.period")}) field-period
                         (label {:text (app/i18n "label.redemption")}) field-amount
                         [button-ok "span, split, tag ok"] [button-cancel "tag cancel"]])])]

  (defn redemption-dialog-ok-action
    []
    (let [period (get-number field-period)
          amount (get-number field-amount)
          redemption (domain/new-redemption period amount)]
      (dosync (ref-set domain/redemptions (conj @domain/redemptions redemption)))
      (.setVisible d false)))
  
  (defn new-redemption-dialog
    "Creates a new redemption dialog."
    []
    (event/add-action-listener button-cancel (action (fn [_] (.setVisible d false))))
    (event/add-action-listener button-ok (action (fn [_] (redemption-dialog-ok-action))))
    (doto d
      (.pack)
      (.setVisible true))))

;;
;; Panels
;;

(defn period-panel
  "Creates the period panel."
  []
  (let [table-model (period-table-model)
        money-cell-renderer (table-cell-renderer (fn [v] (.format app/money-fmt v)) {:horizontalAlignment (swc/swing-keys :right)})
        table (table {:model table-model
                      :gridColor java.awt.Color/DARK_GRAY})
        b-print (button {:action (action (fn [_] (.print table)))
                         :text (app/i18n "button.print")
                         :toolTipText (app/i18n "tooltip.print")})]
    (.setCellRenderer (.getColumn (.getColumnModel table) 2) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 3) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 4) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 5) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 6) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 7) money-cell-renderer)

    (add-watch domain/periods :period-table-update (fn [_ _ _ _] (.fireTableDataChanged table-model)))

    (panel {:layout (mig-layout {:layoutConstraints "wrap 1, insets 10, fill"
                                 :columnConstraints "[grow]"
                                 :rowConstraints "[grow|]"})}
           [[(scroll-pane table) "growx, growy"]
            [b-print "tag right"]])))

(defn input-panel
  "Creates the input panel."
  []
  (let [f-credit (formatted-text-field app/money-fmt {:value (:credit @domain/spec)
                                                  :horizontalAlignment (swc/swing-keys :right)})
        f-rate (formatted-text-field app/money-fmt {:value (:rate @domain/spec)
                                                :horizontalAlignment (swc/swing-keys :right)})
        f-p-interest (formatted-text-field app/money-fmt {:value (:p-interest @domain/spec)
                                                      :horizontalAlignment (swc/swing-keys :right)})
        f-p-redemption (formatted-text-field app/money-fmt {:value (:p-redemption @domain/spec)
                                                        :horizontalAlignment (swc/swing-keys :right)})
        f-term (integer-field {:value (:term @domain/spec)
                               :horizontalAlignment (swc/swing-keys :right)})
        f-rperiod (combo-box {} [(app/i18n "label.redemptionPeriod.annually")
                                 (app/i18n "label.redemptionPeriod.semiannually")
                                 (app/i18n "label.redemptionPeriod.quarterly")
                                 (app/i18n "label.redemptionPeriod.monthly")])
        b-calc (button {:text (app/i18n "button.calculate")})
        b-clear (button {:text (app/i18n "button.clear")})
        b-add-redemption (button {:action (action (fn [_] (new-redemption-dialog)))
                                  :text (app/i18n "button.add")})
        b-remove-redemption (button {;:action (action (fn [_] (remove-redemptions-action)))
                                     :text (app/i18n "button.remove")}) ; TODO remove redemption
        b-clear-redemption (button {:action (action (fn [_] (dosync (ref-set domain/redemptions []))))
                                    :text (app/i18n "button.clear")})
        table-model (redemption-table-model)
        money-cell-renderer (table-cell-renderer (fn [v] (.format app/money-fmt v)) {:horizontalAlignment (swc/swing-keys :right)})
        table (table {:model table-model
                      :gridColor java.awt.Color/DARK_GRAY})]

    (.setCellRenderer (.getColumn (.getColumnModel table) 1) money-cell-renderer)

    (defn read-fields
      []
      (domain/new-spec 
        (get-number f-credit)
        (get-number f-p-interest)
        (get-number f-rate)
        (get-number f-p-redemption)
        (get-number f-term)
        (.getSelectedIndex f-rperiod)
        @domain/redemptions ; vector of ExtraRedemptions
        ))
    
    (defn update-fields
      [spec]
        (.setValue f-credit (domain/financial-rounder (:credit spec)))
        (.setValue f-rate (domain/financial-rounder (:rate spec)))
        (.setValue f-p-interest (domain/financial-rounder (:p-interest spec)))
        (.setValue f-p-redemption (domain/financial-rounder (:p-redemption spec)))
        (.setValue f-term (domain/financial-rounder (:term spec)))
        (.setSelectedIndex f-rperiod (:payment-period spec)))

    (defn calc-action [] (domain/update-spec (domain/calc-spec (read-fields))))
    (defn clear-action [] (domain/update-spec (domain/new-spec)))

    (defn remove-redemptions-action
      []
      (print "Ought to remove rows with indices " (.getSelectedRows table) "."))
    
    ; watch input changes
    (add-watch domain/spec :spec-update (fn [_ _ _ new-spec] (update-fields new-spec)))
    (add-watch domain/spec :spec-calc (fn [_ _ _ new-spec] (domain/update-periods (domain/calc-periods-for-spec new-spec))))
    (add-watch domain/redemptions :redemptions-update (fn [_ _ _ _] (.fireTableDataChanged table-model)))
    (add-watch domain/redemptions :redemptions-spec-calc (fn [_ _ _ _] (domain/update-periods (domain/calc-periods-for-spec (read-fields)))))

    (event/add-action-listener b-calc (action (fn [_] (calc-action))))
    (event/add-action-listener b-clear  (action (fn [_] (clear-action))))

    (panel {:layout (mig-layout {:layoutConstraints "insets 10, wrap 2, fill"
                                 :columnConstraints "[left|grow]"})}
           [[(label {:text (app/i18n "label.data") :font heading-font}) "left, wrap 10"]
            (label {:text (app/i18n "label.amount")}) [f-credit "growx"]
            (label {:text (app/i18n "label.annuity")}) [f-rate "growx"]
            (label {:text (app/i18n "label.interestPercentage")}) [f-p-interest "growx"]
            (label {:text (app/i18n "label.redemptionPercentage")}) [f-p-redemption "growx"]
            (label {:text (app/i18n "label.terms")}) [f-term "growx"]
            (label {:text (app/i18n "label.redemptionPeriod")}) [f-rperiod "growx"]
            [b-calc "span, tag right, split"] [b-clear "tag right, wrap 20"]
            [(label {:text (app/i18n "label.extraRedemptions"):font heading-font}) "left, wrap 10"]
            [(scroll-pane table) "span, growx, growy, wrap"]
            [b-add-redemption "span, tag right, split"] [b-remove-redemption "span, tag right, split"] [b-clear-redemption "tag right"]])))

(defn result-panel
  "Creates the result panel."
  []
  (let [f-years (integer-field {:value (domain/years (count @domain/periods) (nth domain/payments-per-year (:payment-period @domain/spec)))
                                :editable false
                                :horizontalAlignment (swc/swing-keys :right)})
        f-terms (integer-field {:value (count @domain/periods)
                                :editable false
                                :horizontalAlignment (swc/swing-keys :right)})
        f-c-interest (formatted-text-field app/money-fmt {:value (domain/financial-rounder (domain/calc-cumulated-interest @domain/periods))
                                                      :editable false
                                                      :horizontalAlignment (swc/swing-keys :right)})
        f-c-cost (formatted-text-field app/money-fmt {:value (domain/financial-rounder (domain/calc-cumulated-cost @domain/periods))
                                                  :editable false
                                                  :horizontalAlignment (swc/swing-keys :right)})
        p (panel {:layout (mig-layout {:layoutConstraints "wrap 4, insets 10, fill"
                               :columnConstraints "[left|grow|left|grow]"})}
           [[(label {:text (app/i18n "label.summary") :font heading-font}) "left, span, wrap 10"]
            (label {:text (app/i18n "label.years")})      [f-years "growx"]
            (label {:text (app/i18n "label.rates")})      [f-terms "growx"]
            (label {:text (app/i18n "label.c-interest")}) [f-c-interest "growx"]
            (label {:text (app/i18n "label.c-cost")})     [f-c-cost "growx, wrap 20"]
            [(label {:text (app/i18n "label.details"):font heading-font}) "left, span, wrap 10"]
            [(tabbed-pane {}
                         [[(app/i18n "label.redemptionPlan.table") (period-panel)]
                          [(app/i18n "label.term.chart") (jfswing/chart-panel (app/term-chart))]
                          [(app/i18n "label.cumulated.chart") (jfswing/chart-panel (app/cumulated-chart))]
                          [(app/i18n "label.summary") (jfswing/chart-panel (app/redemption-interest-chart))]]
                         ) "span, growx, growy, wrap"]])]

    (defn update-summary
      []
      (set-value f-years (domain/years (count @domain/periods) (nth domain/payments-per-year (:payment-period @domain/spec))))
      (set-value f-terms (count @domain/periods))
      (set-value f-c-interest (domain/financial-rounder (domain/calc-cumulated-interest @domain/periods)))
      (set-value f-c-cost (domain/financial-rounder (domain/calc-cumulated-cost @domain/periods))))
    
    (add-watch domain/periods :summary-update (fn [_ _ _ _] (update-summary)))
  p))

;;
;; Main Menu
;;

(defn main-menu
  "Creates the main menu."
  []
  (menu-bar {}
            [(menu {:text (app/i18n "menu.file")}
                   [(menu-item {:action (action (fn [_] ) ; TODO 
                                                {:name (app/i18n "menu.file.new")
                                                 :accelerator (key-stroke \N :ctrl)
                                                 :mnemonic nil})})
                    (menu-item {:action (action (fn [_] (if-let [file (file-open-dialog ".")]
                                                          (app/load-spec file)))
                                                {:name (app/i18n "menu.file.load")
                                                 :accelerator (key-stroke \O :ctrl)
                                                 :mnemonic nil})})
                    (menu-item {:action (action (fn [_] (app/save-spec @domain/spec))
                                                {:name (app/i18n "menu.file.save")
                                                 :accelerator (key-stroke \S :ctrl)
                                                 :mnemonic nil})})
                    (menu-item {:action (action (fn [_] (if-let [file (file-save-dialog ".")]
                                                           (app/save-spec @domain/spec file)))
                                                {:name (app/i18n "menu.file.saveAs")
                                                 :accelerator (key-stroke \A :ctrl)
                                                 :mnemonic nil})})
                    (separator {})
                    (menu-item {:action (action (fn [_] (System/exit 0))
                                                {:name (app/i18n "menu.file.quit")
                                                 :accelerator (key-stroke \Q :ctrl)
                                                 :mnemonic nil})})])
             (menu {:text (app/i18n "menu.calc")}
                   [(menu-item {:action (action (fn [_] (calc-action))
                                                {:name (app/i18n "menu.calc.calc")})})
                    (menu-item {:action (action (fn [_] (clear-action))
                                                {:name (app/i18n "menu.calc.clear")})})
                    (menu-item {:action (action (fn [_] (generate-pdf-report))
                                                {:name (app/i18n "menu.calc.report.pdf")})})
                    (menu-item {:action (action (fn [_] (generate-excel-report))
                                                {:name (app/i18n "menu.calc.report.excel")})})
                    (menu-item {:action (action (fn [_] (app/save-charts))
                                                {:name (app/i18n "menu.calc.charts")})})])
             (menu {:text (app/i18n "menu.settings")}
                   [(menu {:text (app/i18n "menu.settings.layout")}
                          [(menu-item {:action (action (fn [_] (set-look-and-feel ui-frame :metal))
                                                       {:name (app/i18n "menu.settings.layout.metal") :mnemonic nil})})
                           (menu-item {:action (action (fn [_] (set-look-and-feel ui-frame :nimbus))
                                                       {:name (app/i18n "menu.settings.layout.nimbus") :mnemonic nil})})
                           (menu-item {:action (action (fn [_] (set-look-and-feel ui-frame :synth))
                                                       {:name (app/i18n "menu.settings.layout.synth") :mnemonic nil})})
                           (menu-item {:action (action (fn [_] (set-look-and-feel ui-frame :gtk))
                                                       {:name (app/i18n "menu.settings.layout.gtk") :mnemonic nil})})])
                    (menu {:text (app/i18n "menu.settings.locale")} ; TODO add other locales
                               [(menu-item {:action (action (fn [_] ) ; TODO set locale to de_DE
                                                       {:name (app/i18n "menu.settings.locale.de_DE") :mnemonic nil})})
                                (menu-item {:action (action (fn [_] ) ; TODO set locale to en_GB
                                                       {:name (app/i18n "menu.settings.locale.en_GB") :mnemonic nil})})])])
             (menu {:text (app/i18n "menu.help")}
                   [(menu-item {:action (action (fn [_] (message-dialog (app/i18n "dialog.about.message")
                                                                        (app/i18n "dialog.about.title")
                                                                        :info))
                                                {:name (app/i18n "menu.help.about")
                                                 :accelerator (key-stroke \A :ctrl :alt)
                                                 :mnemonic nil})})])]))

;;
;; Main UI Frame
;;

(defn main-frame
  "Creates the main frame of the user interface."
  []
  (frame {:title (app/i18n "app.title")
          :jMenuBar (main-menu)
          :defaultCloseOperation JFrame/DISPOSE_ON_CLOSE}
         [(vertical-split-pane {}
            [(panel {:layout (mig-layout {:layoutConstraints "wrap 1, insets 10, fill, top"})}
                    [[(input-panel) "grow"]])
             (panel {:layout (mig-layout {:layoutConstraints "wrap 1, insets 10, fill, top"})}
                    [[(result-panel) "grow"]])])]))

(defn init-ui
  "Initializes the user interface."
  []
  (def ui-frame (main-frame))
  (doto ui-frame
    (.pack)
    (.setVisible true)))

(defn start
  "Starts the user interface."
  []
  (do-swing-and-wait (init-ui)))
