;;
;;   Copyright (c) Ludger Solbach. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file license.txt at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;

(ns org.soulspace.annuity.application.annuity
  (:use [clojure.java.io :only [as-file]]
        [org.soulspace.annuity.domain.annuity]
        [org.soulspace.annuity.application i18n]))

;;
;; Application Logic
;;

(def redemption-periods [(i18n "label.redemptionPeriod.annually")
                         (i18n "label.redemptionPeriod.semiannually")
                         (i18n "label.redemptionPeriod.quarterly")
                         (i18n "label.redemptionPeriod.monthly")])

(defn save-spec 
  "Save the credit specification to a file."
  ([spec]
    (save-spec spec "spec.dat"))
  ([spec file]
    (let [spec-data (with-out-str (pr spec))]
      (spit (.getName file) spec-data))))

(defn load-spec
  "Load a credit specification from file."
  [file]
  (update-spec (calc-spec (load-file (.getName file)))))

