(defproject clj_exploration_leaks "0.1.0-SNAPSHOT"
  :description "This project will test the KDE library conexp on a big unstructured dataset from
  https://archive.org/details/datasets_unsorted (03.12.2024)."
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  ; https://stackoverflow.com/a/9917149
  ; $ mvn deploy:deploy-file -Dfile=/Users/klara/Developer/Uni/WiSe2425/conexp-clj/builds/uberjar/conexp-clj-2.6.0-standalone.jar -DartifactId=conexp-clj -Dversion=2.6.0-java17 -DgroupId=conexp-clj -Dpackaging=jar -Durl=file:repository
  :repositories {"local" "file:repository"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [conexp-clj "2.6.0-java17"]                ; FCA library
                 [clojure-interop/java.io "1.0.5"]
                 [plotly-clj "0.1.1"]                       ; visualization
                 ;[org.clojure/java-time "1.3.0"] ; FIXME: dependency resolution error
                 ]
  :repl-options {:init-ns clj-exploration-leaks.core})
