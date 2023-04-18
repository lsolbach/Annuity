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

(ns org.soulspace.annuity.ui.swing
  (:require [clojure.tools.swing-utils :refer [do-swing-and-wait]]
            [org.soulspace.clj.java.awt.core :as awt]
            [org.soulspace.clj.java.awt.events :as aevt]
            [org.soulspace.clj.java.swing.core :as swing]
            [org.soulspace.clj.java.swing.events :as sevt]
            [org.soulspace.cmp.jfreechart.chart :as jfchart]
            [org.soulspace.annuity.domain.annuity :as domain]
            [org.soulspace.annuity.application.annuity :as app]
            [org.soulspace.annuity.application.pdf-report :as rpdf]
            [org.soulspace.annuity.application.excel-report :as rxls])
  (:import [javax.swing Action BorderFactory JFrame]))

;;;;
;;;; Swing UI of the annuity application with clj.swing
;;;;

(declare ui-frame)

(def heading-font (awt/font (awt/font-names :dialog) [(awt/font-styles :bold)] 14))

(defn remove-by-index
  "Removes elem with index 'idx' from coll."
  [coll idx]
  (into (subvec coll 0 idx) (subvec coll (inc idx))))

(defn redemption-table-model
  []
  (swing/mapseq-table-model
    [{:label (app/i18n "label.period") :key :period :editable false}
     {:label (app/i18n "label.redemption")
      :key :amount
      :editable false
      :converter domain/financial-rounder}]
    app/state [:redemptions]))

(defn period-table-model
  []
  (swing/mapseq-table-model
    [{:label (app/i18n "label.period") :key :period :editable false}
     {:label (app/i18n "label.year") :key :year :editable false}
     {:label (app/i18n "label.amountRemaining")
      :key :amount
      :editable false
      :converter domain/financial-rounder}
     {:label (app/i18n "label.rate")
      :key :rate
      :editable false
      :converter domain/financial-rounder}
     {:label (app/i18n "label.interest")
      :key :interest
      :editable false
      :converter domain/financial-rounder}
     {:label (app/i18n "label.redemption")
      :key :redemption
      :editable false
      :converter domain/financial-rounder}
     {:label (app/i18n "label.c-interest")
      :key :c-interest
      :editable false
      :converter domain/financial-rounder}
     {:label (app/i18n "label.c-cost")
      :key :c-cost
      :editable false
      :converter domain/financial-rounder}]
    app/state [:periods]))

;;;
;;; Spec I/O
;;;

(defn open
  "Loads the specification."
  [filename]
  (println (str "Loading " filename))
  (app/load-spec filename))

(defn save
  "Save the specification."
  [filename]
  (println (str "Saving " filename))
  (app/save-spec (:spec @app/state)))

;;;
;;; Dialogs
;;;

(let [field-period (swing/integer-field {:columns 10})
      field-amount (swing/formatted-text-field app/money-fmt {:columns 10 :value 0.0})
      button-ok (swing/button {:text (app/i18n "button.ok")})
      button-cancel (swing/button {:text (app/i18n "button.cancel")})
      d (swing/dialog {:title (app/i18n "dialog.extra-redemption.title")}
                      [(swing/panel {:layout (swing/mig-layout {:layoutConstraints "wrap 2"})}
                                    [(swing/label {:text (app/i18n "label.period")}) field-period
                                     (swing/label {:text (app/i18n "label.redemption")}) field-amount
                                     [button-ok "span, split, tag ok"] [button-cancel "tag cancel"]])])]

  (defn redemption-dialog-ok-action
    []
    (let [period (swing/get-number field-period)
          amount (swing/get-number field-amount)
          redemption (domain/new-redemption period amount)]
      (swap! app/state update :redemptions conj redemption)
      (.setVisible d false)))

  (sevt/add-action-listener button-cancel (swing/action (fn [_] (.setVisible d false))))
  (sevt/add-action-listener button-ok (swing/action (fn [_] (redemption-dialog-ok-action))))

  (defn new-redemption-dialog
    "Creates a new redemption dialog."
    []
    (doto d
      (.pack)
      (.setVisible true))))

;;;
;;; Panels
;;;

(defn period-panel
  "Creates the period panel."
  []
  (let [table-model (period-table-model)
        money-cell-renderer (swing/table-cell-renderer (fn [v]
                                                         (.format app/money-fmt v))
                                                       {:horizontalAlignment (swing/swing-keys :right)})
        table (swing/table {:model table-model
                            :gridColor java.awt.Color/DARK_GRAY})
        b-print (swing/button {:action (swing/action (fn [_] (.print table)))
                         :text (app/i18n "button.print")
                         :toolTipText (app/i18n "tooltip.print")})]
    (.setCellRenderer (.getColumn (.getColumnModel table) 2) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 3) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 4) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 5) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 6) money-cell-renderer)
    (.setCellRenderer (.getColumn (.getColumnModel table) 7) money-cell-renderer)

    (add-watch app/state
               :period-table-update
               (fn [_ _ old-state new-state]
                 (when-not (= (:periods old-state) (:periods new-state))
                   (.fireTableDataChanged table-model))))

    (swing/panel {:layout (swing/mig-layout {:layoutConstraints "wrap 1, insets 10, fill"
                                 :columnConstraints "[grow]"
                                 :rowConstraints "[grow|]"})}
           [[(swing/scroll-pane table) "growx, growy"]
            [b-print "tag right"]])))

(defn input-panel
  "Creates the input panel."
  []
  (let [f-credit (swing/formatted-text-field
                  app/money-fmt {:value (get-in @app/state [:spec :credit])
                                 :horizontalAlignment (swing/swing-keys :right)})
        f-rate (swing/formatted-text-field
                app/money-fmt {:value (get-in @app/state [:spec :rate])
                               :horizontalAlignment (swing/swing-keys :right)})
        f-p-interest (swing/formatted-text-field
                      app/money-fmt {:value (get-in @app/state [:spec :p-interest])
                                     :horizontalAlignment (swing/swing-keys :right)})
        f-p-redemption (swing/formatted-text-field
                        app/money-fmt {:value (get-in @app/state [:spec :p-redemption])
                                       :horizontalAlignment (swing/swing-keys :right)})
        f-term (swing/integer-field {:value (get-in @app/state [:spec :term])
                                     :horizontalAlignment (swing/swing-keys :right)})
        f-rperiod (swing/combo-box {}
                                   [(app/i18n "label.redemptionPeriod.annually")
                                    (app/i18n "label.redemptionPeriod.semiannually")
                                    (app/i18n "label.redemptionPeriod.quarterly")
                                    (app/i18n "label.redemptionPeriod.monthly")])
        b-calc (swing/button {:text (app/i18n "button.calculate")})
        b-clear (swing/button {:text (app/i18n "button.clear")})
        table-model (redemption-table-model)
        money-cell-renderer (swing/table-cell-renderer (fn [v]
                                                         (.format app/money-fmt v))
                                                       {:horizontalAlignment (swing/swing-keys :right)})
        table (swing/table {:model table-model
                            :selectionMode (swing/list-selection-keys :single)
                            :gridColor java.awt.Color/DARK_GRAY})
        list-selection-model (swing/get-selection-model table)
        b-add-redemption (swing/button {:action (swing/action (fn [_] (new-redemption-dialog)))
                                        :text (app/i18n "button.add")})
        b-remove-redemption (swing/button {:action (swing/action
                                                    (fn [_]
                                                      (swap! app/state update :redemptions 
                                                             remove-by-index (.getMinSelectionIndex list-selection-model))))
                                           :text (app/i18n "button.remove")
                                           :enabled false}) ; TODO remove redemption
        b-clear-redemption (swing/button {:action (swing/action
                                                   (fn [_]
                                                     (swap! app/state assoc :redemptions [])))
                                          :text (app/i18n "button.clear")
                                          :enabled false})]

    (.setCellRenderer (.getColumn (.getColumnModel table) 1) money-cell-renderer)
    (-> list-selection-model
        (sevt/add-list-selection-listener
         (sevt/list-selection-listener
          (fn [e] (if-not (.getValueIsAdjusting e)
                    (if (.isSelectionEmpty list-selection-model)
                      (.setEnabled b-remove-redemption false)
                      (.setEnabled b-remove-redemption true)))))))


    (defn read-fields
      []
      (domain/new-spec
       (swing/get-number f-credit)
       (swing/get-number f-p-interest)
       (swing/get-number f-rate)
       (swing/get-number f-p-redemption)
       (swing/get-number f-term)
       (.getSelectedIndex f-rperiod)
       (:redemptions @app/state) ; vector of ExtraRedemptions
       ))

    (defn update-fields
      [spec]
      (.setValue f-credit (domain/financial-rounder (:credit spec)))
      (.setValue f-rate (domain/financial-rounder (:rate spec)))
      (.setValue f-p-interest (domain/financial-rounder (:p-interest spec)))
      (.setValue f-p-redemption (domain/financial-rounder (:p-redemption spec)))
      (.setValue f-term (domain/financial-rounder (:term spec)))
      (.setSelectedIndex f-rperiod (:payment-period spec)))

    (defn calc-action [] (app/update-spec (domain/calc-spec (read-fields))))
    (defn clear-action [] (app/update-spec (domain/new-spec)))

    ; watch input changes
    (add-watch app/state
               :spec-update
               (fn [_ _ old-state new-state]
                 (when-not
                  (= (:spec old-state) (:spec new-state))
                   (update-fields (:spec new-state)))))
    (add-watch app/state
               :spec-calc
               (fn [_ _ old-state new-state]
                 (when-not
                  (= (:spec old-state) (:spec new-state))
                   (app/update-periods (domain/calc-periods-for-spec (:spec new-state))))))
    (add-watch app/state
               :redemptions-update
               (fn [_ _ old-state new-state]
                 (when-not
                  (= (:redemptions old-state) (:redemptions new-state))
                   (.setEnabled b-clear-redemption (not= 0 (count (:redemptions new-state))))
                   (.fireTableDataChanged table-model))))
    (add-watch app/state
               :redemptions-spec-calc
               (fn [_ _ old-state new-state]
                 (when-not
                  (= (:redemptions old-state) (:redemptions new-state))
                   (app/update-periods (domain/calc-periods-for-spec (read-fields))))))

    (sevt/add-action-listener b-calc
                              (swing/action (fn [_]
                                              (calc-action))))
    (sevt/add-action-listener b-clear
                              (swing/action (fn [_]
                                              (clear-action))))

    (swing/panel {:layout (swing/mig-layout {:layoutConstraints "insets 10, wrap 2, fill"
                                             :columnConstraints "[left|grow]"})}
                 [[(swing/label {:text (app/i18n "label.data") :font heading-font}) "left, wrap 10"]
                  (swing/label {:text (app/i18n "label.amount")}) [f-credit "growx"]
                  (swing/label {:text (app/i18n "label.annuity")}) [f-rate "growx"]
                  (swing/label {:text (app/i18n "label.interestPercentage")}) [f-p-interest "growx"]
                  (swing/label {:text (app/i18n "label.redemptionPercentage")}) [f-p-redemption "growx"]
                  (swing/label {:text (app/i18n "label.terms")}) [f-term "growx"]
                  (swing/label {:text (app/i18n "label.redemptionPeriod")}) [f-rperiod "growx"]
                  [b-calc "span, tag right, split"] [b-clear "tag right, wrap 20"]
                  [(swing/label {:text (app/i18n "label.extraRedemptions") :font heading-font}) "left, wrap 10"]
                  [(swing/scroll-pane table) "span, growx, growy, wrap"]
                  [b-add-redemption "span, tag right, split"] [b-remove-redemption "span, tag right, split"] [b-clear-redemption "tag right"]])))

(defn result-panel
  "Creates the result panel."
  []
  (let [f-years (swing/integer-field
                 {:value (domain/years (count (:periods @app/state))
                                       (nth domain/payments-per-year
                                            (:payment-period (:spec @app/state))))
                  :editable false
                  :horizontalAlignment (swing/swing-keys :right)})
        f-terms (swing/integer-field
                 {:value (count (:periods @app/state))
                  :editable false
                  :horizontalAlignment (swing/swing-keys :right)})
        f-c-interest (swing/formatted-text-field
                      app/money-fmt
                      {:value (domain/financial-rounder
                               (domain/calc-cumulated-interest (:periods @app/state)))
                       :editable false
                       :horizontalAlignment (swing/swing-keys :right)})
        f-c-cost (swing/formatted-text-field
                  app/money-fmt {:value (domain/financial-rounder
                                         (domain/calc-cumulated-cost (:periods @app/state)))
                                 :editable false
                                 :horizontalAlignment (swing/swing-keys :right)})
        p (swing/panel {:layout (swing/mig-layout
                                 {:layoutConstraints "wrap 4, insets 10, fill"
                                  :columnConstraints "[left|grow|left|grow]"})}
           [[(swing/label {:text (app/i18n "label.summary") :font heading-font}) "left, span, wrap 10"]
            (swing/label {:text (app/i18n "label.years")})      [f-years "growx"]
            (swing/label {:text (app/i18n "label.rates")})      [f-terms "growx"]
            (swing/label {:text (app/i18n "label.c-interest")}) [f-c-interest "growx"]
            (swing/label {:text (app/i18n "label.c-cost")})     [f-c-cost "growx, wrap 20"]
            [(swing/label {:text (app/i18n "label.details"):font heading-font}) "left, span, wrap 10"]
            [(swing/tabbed-pane {}
                                [[(app/i18n "label.redemptionPlan.table")
                                  (period-panel)]
                                 [(app/i18n "label.term.chart")
                                  (jfchart/chart-panel (app/term-chart (:periods @app/state)))]
                                 [(app/i18n "label.cumulated.chart") 
                                  (jfchart/chart-panel (app/cumulated-chart (:periods @app/state)))]
                                 [(app/i18n "label.summary")
                                  (jfchart/chart-panel (app/redemption-interest-chart (:periods @app/state)))]]) "span, growx, growy, wrap"]])]

    (defn update-summary
      []
      (swing/set-value f-years (domain/years (count (:periods @app/state))
                                             (nth domain/payments-per-year
                                                  (:payment-period (:spec @app/state)))))
      (swing/set-value f-terms (count (:periods @app/state)))
      (swing/set-value f-c-interest (domain/financial-rounder
                                     (domain/calc-cumulated-interest (:periods @app/state))))
      (swing/set-value f-c-cost (domain/financial-rounder
                                 (domain/calc-cumulated-cost (:periods @app/state)))))
    
    (add-watch app/state
               :summary-update
               (fn [_ _ old-state new-state]
                 (when-not
                  (= (:periods old-state) (:periods new-state))
                   (println :summary-update)
                   (update-summary))))
  p))

;;;
;;; Main Menu
;;;

(defn main-menu
  "Creates the main menu."
  []
  (swing/menu-bar {}
            [(swing/menu {:text (app/i18n "menu.file")}
                   [(swing/menu-item {:action (swing/action (fn [_]
                                                              ) ; TODO 
                                                {:name (app/i18n "menu.file.new")
                                                 :accelerator (swing/key-stroke \N :ctrl)
                                                 :mnemonic nil})})
                    (swing/menu-item {:action (swing/action (fn [_]
                                                              (if-let [file (swing/file-open-dialog ".")]
                                                          (app/load-spec file)))
                                                {:name (app/i18n "menu.file.load")
                                                 :accelerator (swing/key-stroke \O :ctrl)
                                                 :mnemonic nil})})
                    (swing/menu-item {:action (swing/action (fn [_]
                                                              (app/save-spec (:spec @app/state)))
                                                {:name (app/i18n "menu.file.save")
                                                 :accelerator (swing/key-stroke \S :ctrl)
                                                 :mnemonic nil})})
                    (swing/menu-item {:action (swing/action (fn [_]
                                                              (if-let [file (swing/file-save-dialog ".")]
                                                           (app/save-spec (:spec @app/state) file)))
                                                {:name (app/i18n "menu.file.saveAs")
                                                 :accelerator (swing/key-stroke \A :ctrl)
                                                 :mnemonic nil})})
                    (swing/separator {})
                    (swing/menu-item {:action (swing/action (fn [_] (System/exit 0))
                                                {:name (app/i18n "menu.file.quit")
                                                 :accelerator (swing/key-stroke \Q :ctrl)
                                                 :mnemonic nil})})])
             (swing/menu {:text (app/i18n "menu.calc")}
                   [(swing/menu-item {:action (swing/action (fn [_]
                                                              (calc-action))
                                                {:name (app/i18n "menu.calc.calc")})})
                    (swing/menu-item {:action (swing/action (fn [_]
                                                              (clear-action))
                                                {:name (app/i18n "menu.calc.clear")})})
                    (swing/menu-item {:action (swing/action (fn [_]
                                                              (rpdf/generate-pdf-report @app/state))
                                                {:name (app/i18n "menu.calc.report.pdf")})})
                    (swing/menu-item {:action (swing/action (fn [_]
                                                              (rxls/generate-excel-report @app/state))
                                                {:name (app/i18n "menu.calc.report.excel")})})
                    (swing/menu-item {:action (swing/action (fn [_]
                                                              (app/save-charts))
                                                {:name (app/i18n "menu.calc.charts")})})])
             (swing/menu {:text (app/i18n "menu.settings")}
                   [(swing/menu {:text (app/i18n "menu.settings.layout")}
                          [(swing/menu-item {:action (swing/action (fn [_]
                                                                     (swing/set-look-and-feel ui-frame :metal))
                                                       {:name (app/i18n "menu.settings.layout.metal") :mnemonic nil})})
                           (swing/menu-item {:action (swing/action (fn [_]
                                                                     (swing/set-look-and-feel ui-frame :nimbus))
                                                       {:name (app/i18n "menu.settings.layout.nimbus") :mnemonic nil})})
                           (swing/menu-item {:action (swing/action (fn [_]
                                                                     (swing/set-look-and-feel ui-frame :synth))
                                                       {:name (app/i18n "menu.settings.layout.synth") :mnemonic nil})})
                           (swing/menu-item {:action (swing/action (fn [_]
                                                                     (swing/set-look-and-feel ui-frame :gtk))
                                                       {:name (app/i18n "menu.settings.layout.gtk") :mnemonic nil})})])
                    (swing/menu {:text (app/i18n "menu.settings.locale")} ; TODO add other locales
                               [(swing/menu-item {:action (swing/action (fn [_]
                                                                          ) ; TODO set locale to de_DE
                                                       {:name (app/i18n "menu.settings.locale.de_DE") :mnemonic nil})})
                                (swing/menu-item {:action (swing/action (fn [_]
                                                                          ) ; TODO set locale to en_GB
                                                       {:name (app/i18n "menu.settings.locale.en_GB") :mnemonic nil})})])])
             (swing/menu {:text (app/i18n "menu.help")}
                   [(swing/menu-item {:action (swing/action (fn [_]
                                                              (swing/message-dialog (app/i18n "dialog.about.message")
                                                                                    (app/i18n "dialog.about.title")
                                                                                    :info))
                                                {:name (app/i18n "menu.help.about")
                                                 :accelerator (swing/key-stroke \A :ctrl :alt)
                                                 :mnemonic nil})})])]))

;;;
;;; Main UI Frame
;;;

(defn main-frame
  "Creates the main frame of the user interface."
  []
  (swing/frame {:title (app/i18n "app.title")
          :jMenuBar (main-menu)
          :defaultCloseOperation JFrame/DISPOSE_ON_CLOSE}
         [(swing/vertical-split-pane {}
            [(swing/panel {:layout (swing/mig-layout {:layoutConstraints "wrap 1, insets 10, fill, top"})}
                    [[(input-panel) "grow"]])
             (swing/panel {:layout (swing/mig-layout {:layoutConstraints "wrap 1, insets 10, fill, top"})}
                    [[(result-panel) "grow"]])])]))

;;;
;;; UI initialization
;;;

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
