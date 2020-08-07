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
  (:require [clojure.tools.swing-utils :refer [do-swing-and-wait]])
  (:use [org.soulspace.clj.java awt]
        [org.soulspace.clj.java.awt event]
        [org.soulspace.clj.java.swing constants swinglib]
        [org.soulspace.math.interest]
        [org.soulspace.cmp.jfreechart swing]
        [org.soulspace.annuity.domain annuity]
        [org.soulspace.annuity.application annuity i18n formats charts pdf-report excel-report])
  (:import [javax.swing Action BorderFactory JFrame]))

;;
;; Swing UI of the annuity application with clj.swing
;;

(declare ui-frame)

(def heading-font (font (font-names :dialog) [(font-styles :bold)] 14))

(defn redemption-table-model
  []
  (mapseq-table-model
    [{:label (i18n "label.period") :key :period :edit false}
     {:label (i18n "label.redemption") :key :amount :edit false :converter financial-rounder}]
    redemptions))

(defn period-table-model
  []
  (mapseq-table-model
    [{:label (i18n "label.period") :key :period :edit false}
     {:label (i18n "label.year") :key :year :edit false}
     {:label (i18n "label.amountRemaining") :key :amount :edit false :converter financial-rounder}
     {:label (i18n "label.rate") :key :rate :edit false :converter financial-rounder}
     {:label (i18n "label.interest") :key :interest :edit false :converter financial-rounder}
     {:label (i18n "label.redemption") :key :redemption :edit false :converter financial-rounder}
     {:label (i18n "label.c-interest") :key :c-interest :edit false :converter financial-rounder}
     {:label (i18n "label.c-cost") :key :c-cost :edit false :converter financial-rounder}]
    periods))

(defn open
  "Loads the specification."
  [filename]
  (println (str "Loading " filename))
  (load-spec filename))

(defn save
  "Save the specification."
  [filename]
  (println (str "Saving " filename))
  (save-spec @spec))

;;
;; Dialogs
;;

(let [field-period (integer-field {:columns 10})
      field-amount (formatted-text-field money-fmt {:columns 10 :value 0.0})
      button-ok (button {:text (i18n "button.ok")})
      button-cancel (button {:text (i18n "button.cancel")})
      d (dialog {:title (i18n "dialog.extra-redemption.title")}                  
                [(panel {:layout (mig-layout {:layoutConstraints "wrap 2"})}
                        [(label {:text (i18n "label.period")}) field-period
                         (label {:text (i18n "label.redemption")}) field-amount
                         [button-ok "span, split, tag ok"] [button-cancel "tag cancel"]])])]

  (defn redemption-dialog-ok-action
    []
    (let [period (get-number field-period)
          amount (get-number field-amount)
          redemption (new-redemption period amount)]
      (dosync (ref-set redemptions (conj @redemptions redemption)))
      (.setVisible d false)))
  
  (defn new-redemption-dialog
    "Creates a new redemption dialog."
    []
    (add-action-listener button-cancel (action (fn [_] (.setVisible d false))))
    (add-action-listener button-ok (action (fn [_] (redemption-dialog-ok-action))))
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
        money-cell-renderer (table-cell-renderer (fn [v] (.format money-fmt v)) {:horizontalAlignment (swing-keys :right)})
        table (table {:model table-model
                      :gridColor java.awt.Color/DARK_GRAY})
        b-print (button {:action (action (fn [_] (.print table)))
                         :text (i18n "button.print")
                         :toolTipText (i18n "tooltip.print")})]
    (.setCellRenderer (.getColumn (.getColumnModel table) 2) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 3) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 4) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 5) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 6) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 7) money-cell-renderer)

    (add-watch periods :period-table-update (fn [_ _ _ _] (.fireTableDataChanged table-model)))

    (panel {:layout (mig-layout {:layoutConstraints "wrap 1, insets 10, fill"
                                 :columnConstraints "[grow]"
                                 :rowConstraints "[grow|]"})}
           [[(scroll-pane table) "growx, growy"]
            [b-print "tag right"]])))

(defn input-panel
  "Creates the input panel."
  []
  (let [f-credit (formatted-text-field money-fmt {:value (:credit @spec)
                                                  :horizontalAlignment (swing-keys :right)})
        f-rate (formatted-text-field money-fmt {:value (:rate @spec)
                                                :horizontalAlignment (swing-keys :right)})
        f-p-interest (formatted-text-field money-fmt {:value (:p-interest @spec)
                                                      :horizontalAlignment (swing-keys :right)})
        f-p-redemption (formatted-text-field money-fmt {:value (:p-redemption @spec)
                                                        :horizontalAlignment (swing-keys :right)})
        f-term (integer-field {:value (:term @spec)
                               :horizontalAlignment (swing-keys :right)})
        f-rperiod (combo-box {} [(i18n "label.redemptionPeriod.annually")
                                 (i18n "label.redemptionPeriod.semiannually")
                                 (i18n "label.redemptionPeriod.quarterly")
                                 (i18n "label.redemptionPeriod.monthly")])
        b-calc (button {:text (i18n "button.calculate")})
        b-clear (button {:text (i18n "button.clear")})
        b-add-redemption (button {:action (action (fn [_] (new-redemption-dialog)))
                                  :text (i18n "button.add")})
        b-remove-redemption (button {;:action (action (fn [_] (remove-redemptions-action)))
                                     :text (i18n "button.remove")}) ; TODO remove redemption
        b-clear-redemption (button {:action (action (fn [_] (dosync (ref-set redemptions []))))
                                    :text (i18n "button.clear")})
        table-model (redemption-table-model)
        money-cell-renderer (table-cell-renderer (fn [v] (.format money-fmt v)) {:horizontalAlignment (swing-keys :right)})
        table (table {:model table-model
                      :gridColor java.awt.Color/DARK_GRAY})]

    (.setCellRenderer (.getColumn (.getColumnModel table) 1) money-cell-renderer)

    (defn read-fields
      []
      (new-spec 
        (get-number f-credit)
        (get-number f-p-interest)
        (get-number f-rate)
        (get-number f-p-redemption)
        (get-number f-term)
        (.getSelectedIndex f-rperiod)
        @redemptions ; vector of ExtraRedemptions
        ))
    
    (defn update-fields
      [spec]
        (.setValue f-credit (financial-rounder (:credit spec)))
        (.setValue f-rate (financial-rounder (:rate spec)))
        (.setValue f-p-interest (financial-rounder (:p-interest spec)))
        (.setValue f-p-redemption (financial-rounder (:p-redemption spec)))
        (.setValue f-term (financial-rounder (:term spec)))
        (.setSelectedIndex f-rperiod (:payment-period spec)))

    (defn calc-action [] (update-spec (calc-spec (read-fields))))
    (defn clear-action [] (update-spec (new-spec)))

    (defn remove-redemptions-action
      []
      (print "Ought to remove rows with indices " (.getSelectedRows table) "."))
    
    ; watch input changes
    (add-watch spec :spec-update (fn [_ _ _ new-spec] (update-fields new-spec)))
    (add-watch spec :spec-calc (fn [_ _ _ new-spec] (update-periods (calc-periods-for-spec new-spec))))
    (add-watch redemptions :redemptions-update (fn [_ _ _ _] (.fireTableDataChanged table-model)))
    (add-watch redemptions :redemptions-spec-calc (fn [_ _ _ _] (update-periods (calc-periods-for-spec (read-fields)))))

    (add-action-listener b-calc (action (fn [_] (calc-action))))
    (add-action-listener b-clear  (action (fn [_] (clear-action))))

    (panel {:layout (mig-layout {:layoutConstraints "insets 10, wrap 2, fill"
                                 :columnConstraints "[left|grow]"})}
           [[(label {:text (i18n "label.data") :font heading-font}) "left, wrap 10"]
            (label {:text (i18n "label.amount")}) [f-credit "growx"]
            (label {:text (i18n "label.annuity")}) [f-rate "growx"]
            (label {:text (i18n "label.interestPercentage")}) [f-p-interest "growx"]
            (label {:text (i18n "label.redemptionPercentage")}) [f-p-redemption "growx"]
            (label {:text (i18n "label.terms")}) [f-term "growx"]
            (label {:text (i18n "label.redemptionPeriod")}) [f-rperiod "growx"]
            [b-calc "span, tag right, split"] [b-clear "tag right, wrap 20"]
            [(label {:text (i18n "label.extraRedemptions"):font heading-font}) "left, wrap 10"]
            [(scroll-pane table) "span, growx, growy, wrap"]
            [b-add-redemption "span, tag right, split"] [b-remove-redemption "span, tag right, split"] [b-clear-redemption "tag right"]])))

(defn result-panel
  "Creates the result panel."
  []
  (let [f-years (integer-field {:value (years (count @periods) (nth payments-per-year (:payment-period @spec)))
                                :editable false
                                :horizontalAlignment (swing-keys :right)})
        f-terms (integer-field {:value (count @periods)
                                :editable false
                                :horizontalAlignment (swing-keys :right)})
        f-c-interest (formatted-text-field money-fmt {:value (financial-rounder (calc-cumulated-interest @periods))
                                                      :editable false
                                                      :horizontalAlignment (swing-keys :right)})
        f-c-cost (formatted-text-field money-fmt {:value (financial-rounder (calc-cumulated-cost @periods))
                                                  :editable false
                                                  :horizontalAlignment (swing-keys :right)})
        p (panel {:layout (mig-layout {:layoutConstraints "wrap 4, insets 10, fill"
                               :columnConstraints "[left|grow|left|grow]"})}
           [[(label {:text (i18n "label.summary") :font heading-font}) "left, span, wrap 10"]
            (label {:text (i18n "label.years")})      [f-years "growx"]
            (label {:text (i18n "label.rates")})      [f-terms "growx"]
            (label {:text (i18n "label.c-interest")}) [f-c-interest "growx"]
            (label {:text (i18n "label.c-cost")})     [f-c-cost "growx, wrap 20"]
            [(label {:text (i18n "label.details"):font heading-font}) "left, span, wrap 10"]
            [(tabbed-pane {}
                         [[(i18n "label.redemptionPlan.table") (period-panel)]
                          [(i18n "label.term.chart") (chart-panel (term-chart))]
                          [(i18n "label.cumulated.chart") (chart-panel (cumulated-chart))]
                          [(i18n "label.summary") (chart-panel (redemption-interest-chart))]]
                         ) "span, growx, growy, wrap"]])]

    (defn update-summary
      []
      (set-value f-years (years (count @periods) (nth payments-per-year (:payment-period @spec))))
      (set-value f-terms (count @periods))
      (set-value f-c-interest (financial-rounder (calc-cumulated-interest @periods)))
      (set-value f-c-cost (financial-rounder (calc-cumulated-cost @periods))))
    
    (add-watch periods :summary-update (fn [_ _ _ _] (update-summary)))
  p))

;;
;; Main Menu
;;

(defn main-menu
  "Creates the main menu."
  []
  (menu-bar {}
            [(menu {:text (i18n "menu.file")}
                   [(menu-item {:action (action (fn [_] ) ; TODO 
                                                {:name (i18n "menu.file.new")
                                                 :accelerator (key-stroke \N :ctrl)
                                                 :mnemonic nil})})
                    (menu-item {:action (action (fn [_] (if-let [file (file-open-dialog ".")]
                                                          (load-spec file)))
                                                {:name (i18n "menu.file.load")
                                                 :accelerator (key-stroke \O :ctrl)
                                                 :mnemonic nil})})
                    (menu-item {:action (action (fn [_] (save-spec @spec))
                                                {:name (i18n "menu.file.save")
                                                 :accelerator (key-stroke \S :ctrl)
                                                 :mnemonic nil})})
                    (menu-item {:action (action (fn [_] (if-let [file (file-save-dialog ".")]
                                                           (save-spec @spec file)))
                                                {:name (i18n "menu.file.saveAs")
                                                 :accelerator (key-stroke \A :ctrl)
                                                 :mnemonic nil})})
                    (separator {})
                    (menu-item {:action (action (fn [_] (System/exit 0))
                                                {:name (i18n "menu.file.quit")
                                                 :accelerator (key-stroke \Q :ctrl)
                                                 :mnemonic nil})})])
             (menu {:text (i18n "menu.calc")}
                   [(menu-item {:action (action (fn [_] (calc-action))
                                                {:name (i18n "menu.calc.calc")})})
                    (menu-item {:action (action (fn [_] (clear-action))
                                                {:name (i18n "menu.calc.clear")})})
                    (menu-item {:action (action (fn [_] (generate-pdf-report))
                                                {:name (i18n "menu.calc.report.pdf")})})
                    (menu-item {:action (action (fn [_] (generate-excel-report))
                                                {:name (i18n "menu.calc.report.excel")})})
                    (menu-item {:action (action (fn [_] (save-charts))
                                                {:name (i18n "menu.calc.charts")})})])
             (menu {:text (i18n "menu.settings")}
                   [(menu {:text (i18n "menu.settings.layout")}
                          [(menu-item {:action (action (fn [_] (set-look-and-feel ui-frame :metal))
                                                       {:name (i18n "menu.settings.layout.metal") :mnemonic nil})})
                           (menu-item {:action (action (fn [_] (set-look-and-feel ui-frame :nimbus))
                                                       {:name (i18n "menu.settings.layout.nimbus") :mnemonic nil})})
                           (menu-item {:action (action (fn [_] (set-look-and-feel ui-frame :synth))
                                                       {:name (i18n "menu.settings.layout.synth") :mnemonic nil})})
                           (menu-item {:action (action (fn [_] (set-look-and-feel ui-frame :gtk))
                                                       {:name (i18n "menu.settings.layout.gtk") :mnemonic nil})})])
                    (menu {:text (i18n "menu.settings.locale")} ; TODO add other locales
                               [(menu-item {:action (action (fn [_] ) ; TODO set locale to de_DE
                                                       {:name (i18n "menu.settings.locale.de_DE") :mnemonic nil})})
                                (menu-item {:action (action (fn [_] ) ; TODO set locale to en_GB
                                                       {:name (i18n "menu.settings.locale.en_GB") :mnemonic nil})})])])
             (menu {:text (i18n "menu.help")}
                   [(menu-item {:action (action (fn [_] (message-dialog (i18n "dialog.about.message")
                                                                        (i18n "dialog.about.title")
                                                                        :info))
                                                {:name (i18n "menu.help.about")
                                                 :accelerator (key-stroke \A :ctrl :alt)
                                                 :mnemonic nil})})])]))

;;
;; Main UI Frame
;;

(defn main-frame
  "Creates the main frame of the user interface."
  []
  (frame {:title (i18n "app.title")
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
