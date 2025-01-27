(ns clj-exploration-leaks.exploration
  (:require [clj-exploration-leaks.io.directory :as dir]
            [clj-exploration-leaks.io.metadata :as metadata]
            [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [conexp.fca.many-valued-contexts :as mv_contexts]
            [clojure.java.io :as io]
            [clj-exploration-leaks.fca-dir :as fca-dir]))

;; run in terminal using "lein run -m clj-exploration-leaks.exploration"
(defn -main
  [& args]

  ;(def save_dir "results/")
  ;(def date "090125")
  ;(let [file_path "/norgay/bigstore/kgu/data/ETYNTKE" ; "/Users/klara/Downloads"
  ;      ]
  ;; metadata
  ;(metadata/write-metadata-csv (metadata/find_metadata file_path) save_dir (str "metadata_" date ".csv"))
  ;; file tree
  ;(dir/save-file-tree file_path save_dir (str "file-tree_" date ".txt"))
  ;)



  ; displays metadata
  ;(let [file_path (str save_dir (str "metadata_" date ".csv"))
  ;      ctx_map (csv2ctx/extract-obj-attr-inc file_path)    ; for multi-valued context
  ;      bin-ctx-map (csv2ctx/extract-obj-attr-inc-binary file_path) ; binary context without updated incidence
  ;      updated-bin-ctx-map (csv2ctx/update-incidence (nth bin-ctx-map 2)) ; only updated incidence context
  ;      ; merged updated incidence-binary-context into sequence of binary contexts
  ;      complete-updated-bin-ctx-map (csv2ctx/insert-updated-ctx-into-ctx-seq bin-ctx-map updated-bin-ctx-map 2)
  ;      ]
  ;
  ;  (with-open [writer (io/writer (str save_dir "multi-valued-" date ".edn"))]
  ;    (binding [*out* writer]
  ;      (prn ctx_map)))
  ;  (println "saved multi-valued context to .edn file")
  ;
  ;  (with-open [writer (io/writer (str save_dir "complete-old-binary-valued-" date ".edn"))]
  ;    (binding [*out* writer]
  ;      (prn bin-ctx-map)))
  ;  (println "saved complete-old-binary-valued context to .edn file")
  ;
  ;
  ;  (with-open [writer (io/writer (str save_dir "complete-updated-binary-valued-" date ".edn"))]
  ;    (binding [*out* writer]
  ;      (prn complete-updated-bin-ctx-map)))
  ;  (println "saved complete-updated-binary-valued context to .edn file")

    ;(println "-----------------")
    ;(println "Multi-valued" (mv_contexts/make-mv-context-from-matrix (:objects ctx_map) (:attributes ctx_map) (:incidence ctx_map)))
    ;(println "-----------------")
    ;(println "Binary-valued:")
    ;(csv2ctx/display-bin-ctx (nth bin-ctx-map 2)) (nth bin-ctx-map 2)
    ;(println "-----------------")
    ;(println "updated binary-valued:")
    ;(csv2ctx/display-bin-ctx updated-bin-ctx-map)
    ;(println "-----------------")
    ;(println "Insert updated binary-valued:")
    ;(csv2ctx/display-bin-ctx complete-updated-bin-ctx-map)
    ;(println "-----------------")
    ;(println "binary-valued" bin-ctx-map)
    ;(println "-----------------")
    ;(println "updated binary-valued" complete-updated-bin-ctx-map)
    ;(println "-----------------")


    )

  (println "-----------------")


  ; save statistics (frequencies) of data in .CSV and .txt files
  ;(let [metadata_path (str save_dir (str "metadata_" date ".csv"))
  ;      stats (metadata/get-stats metadata_path)]
  ;(println "saved stats to txt file:" (metadata/write-stats2txt stats (str save_dir (str "stats_" date ".txt"))))
  ;(println "-----------------")
  ;(println "saved stats to csv file:" (metadata/create-column-stats-files stats save_dir)))


  ; create dir-topic context across multiple directories
  (let [path2csv-files "/norgay/bigstore/kgu/dev/text_topic/results/fca/01_27_25/"
        concepts-save-path "/norgay/bigstore/kgu/dev/clj_exploration_leaks/results/fca-dir-concepts/"
        output-save-path "/norgay/bigstore/kgu/dev/clj_exploration_leaks/results/fca-dir-concepts/across-dir/"
        across-dir-ctx (csv2ctx/zero-one-csv2-map (str output-save-path "across-dir-incidence-matrix.csv"))
        objects (:objects across-dir-ctx)
        attributes (:attributes across-dir-ctx)
        incidence (:incidence across-dir-ctx)
        ;ctx (contexts/make-context-from-matrix objects attributes incidence)
      ]
  ;; create csv file containing incidence matrix of subdirectories and topics
    (fca-dir/subdir-topic-inc2ctx-csv path2csv-files concepts-save-path output-save-path)

  ;; display context
  ;  (println "across-dir-ctx" across-dir-ctx)
  ;  (csv2ctx/display-bin-ctx across-dir-ctx)
  ;  (println "objects" objects)
  ;  (println "attributes" attributes)
  ;  (println "incidence" incidence)
  ;  (csv2ctx/compute-titanic-iceberg-lattice ctx)
  )
