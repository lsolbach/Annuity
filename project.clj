(defproject org.soulspace.finance/AnnuityApp "0.3.0-SNAPSHOT"
  :description "An application to calculate and visualize the costs and redemption plans for annuity credits"
  :url "https://github.com/lsolbach/CljXML"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :main org.soulspace.annuity.application

;  ; use deps.edn dependencies
;  :plugins [[lein-tools-deps "0.4.5"]]
;  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
;  :lein-tools-deps/config {:config-files [:install :user :project]}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.soulspace.clj/clj.base "0.8.3"]
                 [org.soulspace.clj/clj.java "0.8.0"]
                 [org.soulspace.clj/clj.swing "0.7.3"]
                 [org.soulspace.clj/clj.app "0.7.0"]
                 [org.soulspace.clj/cmp.batik "0.2.0"]
                 [org.soulspace.clj/cmp.fop "0.3.0"]
                 [org.soulspace.clj/cmp.jfreechart "0.4.0"]
                 [org.soulspace.clj/cmp.poi "0.6.1"]
                 [org.soulspace.clj/math.financial "0.5.0"]
                 [org.soulspace.clj/xml.core "0.5.0"]
                 [org.soulspace.clj/xml.dsl "0.5.2"]]
  :test-paths ["test"])
