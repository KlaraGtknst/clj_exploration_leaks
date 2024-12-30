(ns clj-exploration-leaks.exploration
  (:require [clj-exploration-leaks.io.directory :as dir]
            [clj-exploration-leaks.io.metadata :as metadata]
            [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [conexp.fca.many-valued-contexts :as mv_contexts]))


(defn -main                                                 ; run in terminal using "lein run -m clj-exploration-leaks.exploration"
  [& args]

  (def save_dir "results/")
  (def date "301224")
  (let [file_path "/Users/klara/Downloads"                  ;"/norgay/bigstore/kgu/data/ETYNTKE" ;             ; TODO: change to your file path
        ]
    ;(dir/save-file-tree file_path save_dir)
  (metadata/write-metadata-csv (metadata/find_metadata file_path) save_dir (str "metadata_" date ".csv"))
  (dir/save-file-tree file_path save_dir (str "file-tree_" date ".csv"))
  )



  ; displays metadata as multi-valued context
  (let [file_path (str save_dir (str "metadata_" date ".csv"))
        ctx_map (csv2ctx/extract-obj-attr-inc file_path)
        bin-ctx-map (csv2ctx/extract-obj-attr-inc-binary file_path)
        updated-bin-ctx-map (map-indexed (fn [idx elem]
                                           (if (= idx 2)
                                             (csv2ctx/update-incidence elem)
                                             elem))
                                         bin-ctx-map)       ;
        ; (assoc (vec bin-ctx-map) 2 (csv2ctx/update-incidence (nth bin-ctx-map 2)))
        ]
    (println "Multi-valued" (mv_contexts/make-mv-context-from-matrix (:objects ctx_map) (:attributes ctx_map) (:incidence ctx_map)))
    ;(println "Binary-valued" (csv2ctx/display-bin-ctx bin-ctx-map))
    (println "START")
    (println "updated binary-valued" (nth updated-bin-ctx-map 2) (csv2ctx/display-bin-ctx (nth updated-bin-ctx-map 2)))
    (println "END")
    (println "updated binary-valued" (count (filter #(= % 1) (:incidence (nth updated-bin-ctx-map 2))))
             (count (filter #(= % 1) (:incidence (nth bin-ctx-map 2)))))
    ;(println (type updated-bin-ctx-map ) (type bin-ctx-map))
    )


  ; save statistics (frequencies) of data in .CSV and .txt files
  ;(let [metadata_path (str save_dir (str "metadata_" date ".csv"))
  ;      stats (metadata/get-stats metadata_path)]
  ;(println "saved stats to txt file:" (metadata/write-stats2txt stats (str save_dir (str "stats_" date ".txt"))))
  ;(println "saved stats to csv file:" (metadata/create-column-stats-files stats save_dir))))

