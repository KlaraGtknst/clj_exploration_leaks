(defproject clj_exploration_leaks "0.1.0-SNAPSHOT"
  :description "This project will test the KDE library conexp on a big unstructured dataset from
  https://archive.org/details/datasets_unsorted (03.12.2024)."
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [conexp-clj "2.6.0"]                ; FCA library
                 [clojure-interop/java.io "1.0.5"]
                 [plotly-clj "0.1.1"]                       ; visualization
                 ;[org.clojure/java-time "1.3.0"] ; FIXME: dependency resolution error
                 ]
  :repl-options {:init-ns clj-exploration-leaks.core})
