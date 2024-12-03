(ns clj-exploration-leaks.exploration
  (:require [clj-exploration-leaks.io.directory :as dir]
            [clj-exploration-leaks.io.metadata :as metadata]
            [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [conexp.fca.many-valued-contexts :as mv_contexts]))

(def save_dir "results/")
(let [file_path "/Users/klara/Downloads"]                                ; TODO: change to your file path
  ;(dir/save-file-tree file_path save_dir)
(metadata/write-metadata-csv (metadata/find_metadata file_path) save_dir "metadata.csv")
(dir/save-file-tree file_path save_dir "file-tree031224.txt")
)

; displays metadata as multi-valued context
(let [file_path (str save_dir "metadata.csv")
      ctx_map (csv2ctx/extract-obj-attr-inc file_path)]
  (println (mv_contexts/make-mv-context-from-matrix (:objects ctx_map) (:attributes ctx_map) (:incidence ctx_map))))

; save statistics (frequencies) of data in .CSV and .txt files
(let [metadata_path (str save_dir "metadata.csv")
      stats (metadata/get-stats metadata_path)]
(println (metadata/write-stats2txt stats (str save_dir "stats.txt")))
(println (metadata/create-column-stats-files stats save_dir)))