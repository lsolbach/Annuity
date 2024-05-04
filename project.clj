(defproject org.soulspace.finance/Annuity "0.3.0-SNAPSHOT"
  :description "An application to calculate and visualize the costs and redemption plans for annuity credits"
  :url "https://github.com/lsolbach/Annuity"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :main org.soulspace.annuity.application

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.soulspace.clj/clj.swing "0.8.0"]
                 [org.soulspace.clj/cmp.batik "0.2.1"]
                 [org.soulspace.clj/cmp.fop "0.3.1"]
                 [org.soulspace.clj/cmp.jfreechart "0.5.0"]
                 [org.soulspace.clj/cmp.poi "0.6.4"]
                 [org.soulspace.clj/math.financial "0.7.0"]
                 [org.soulspace.clj/xml.core "0.5.1"]
                 [org.soulspace.clj/xml.dsl "0.5.2"]]

  :test-paths ["test"]
  :repl-options {:init-ns org.soulspace.annuity.application}

  
  :profiles {:dev {:dependencies [[djblue/portal "0.49.1"]
                                  [criterium/criterium "0.4.6"]
                                  [com.clojure-goes-fast/clj-java-decompiler "0.3.4"]
                                    ; [expound/expound "0.9.0"]
                                  ]
                   :global-vars {*warn-on-reflection* true}}}



;  :profiles {:uberjar {:aot [org.soulspace.annuity.application]}}
  )
