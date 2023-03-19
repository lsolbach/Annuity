(defproject org.soulspace.finance/Annuity "0.3.0-SNAPSHOT"
  :description "An application to calculate and visualize the costs and redemption plans for annuity credits"
  :url "https://github.com/lsolbach/Annuity"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :main org.soulspace.annuity.application

  ; use deps.edn dependencies
  :plugins [[lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}

  :test-paths ["test"])
