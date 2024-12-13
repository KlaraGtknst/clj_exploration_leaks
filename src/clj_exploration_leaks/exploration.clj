(ns clj-exploration-leaks.exploration
  (:require [clj-exploration-leaks.io.directory :as dir]
            [clj-exploration-leaks.io.metadata :as metadata]
            [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [conexp.fca.many-valued-contexts :as mv_contexts]))

(def save_dir "results/")
(let [file_path "/norgay/bigstore/kgu/data/ETYNTKE"]                   ;    "/Users/klara/Downloads"         ; TODO: change to your file path
  ;(dir/save-file-tree file_path save_dir)
(metadata/write-metadata-csv (metadata/find_metadata file_path) save_dir "metadata_131224.csv")
(dir/save-file-tree file_path save_dir "file-tree131224.txt")
)

; displays metadata as multi-valued context
(let [file_path (str save_dir "metadata_131224.csv")
      ctx_map (csv2ctx/extract-obj-attr-inc file_path)]
  (println "Multi-valued" (mv_contexts/make-mv-context-from-matrix (:objects ctx_map) (:attributes ctx_map) (:incidence ctx_map)))
  (println "Binary-valued" (csv2ctx/display-bin-ctx (csv2ctx/extract-obj-attr-inc-binary file_path))))


; save statistics (frequencies) of data in .CSV and .txt files
(let [metadata_path (str save_dir "metadata_131224.csv")
      stats (metadata/get-stats metadata_path)]
(println (metadata/write-stats2txt stats (str save_dir "stats_131224.txt")))
(println (metadata/create-column-stats-files stats save_dir)))