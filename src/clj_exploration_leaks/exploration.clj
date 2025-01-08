(ns clj-exploration-leaks.exploration
  (:require [clj-exploration-leaks.io.directory :as dir]
            [clj-exploration-leaks.io.metadata :as metadata]
            [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [conexp.fca.many-valued-contexts :as mv_contexts]
            [clojure.java.io :as io]))


(defn -main                                                 ; run in terminal using "lein run -m clj-exploration-leaks.exploration"
  [& args]

  (def save_dir "results/")
  (def date "030125")
  (let [file_path "/norgay/bigstore/kgu/data/ETYNTKE/Physics" ; "/Users/klara/Downloads"                  ;            ; TODO: change to your file path
        ]
    ;(dir/save-file-tree file_path save_dir)
  (metadata/write-metadata-csv (metadata/find_metadata file_path) save_dir (str "metadata_" date ".csv"))
  (dir/save-file-tree file_path save_dir (str "file-tree_" date ".csv"))
  )



  ; displays metadata as multi-valued context
  (let [file_path (str save_dir (str "metadata_" date ".csv"))
        ctx_map (csv2ctx/extract-obj-attr-inc file_path)
        bin-ctx-map (csv2ctx/extract-obj-attr-inc-binary file_path)
        updated-bin-ctx-map (csv2ctx/update-incidence (nth bin-ctx-map 2))
        upt (update (vec bin-ctx-map) 2 #(assoc % :incidence (:incidence updated-bin-ctx-map))) ; fixme: Doesn't work, empty context
        ]
    ;(println "Multi-valued" (mv_contexts/make-mv-context-from-matrix (:objects ctx_map) (:attributes ctx_map) (:incidence ctx_map)))
    ;(println "Binary-valued" (csv2ctx/display-bin-ctx (nth bin-ctx-map 2)) (nth bin-ctx-map 2))
    ;(println "updated binary-valued" (csv2ctx/display-bin-ctx updated-bin-ctx-map))
    (csv2ctx/display-bin-ctx upt)
    (println "binary-valued" bin-ctx-map)
    (println "updated binary-valued" upt)

    (with-open [writer (io/writer (str save_dir "multi-valued-" date ".edn"))]
      (binding [*out* writer]
        (prn ctx_map)))

    (with-open [writer (io/writer (str save_dir "binary-valued-" date ".edn"))]
      (binding [*out* writer]
        (prn bin-ctx-map)))

    (with-open [writer (io/writer (str save_dir "updated-binary-valued-" date ".edn"))]
      (binding [*out* writer]
        (prn updated-bin-ctx-map)))
    )


  ; save statistics (frequencies) of data in .CSV and .txt files
  ;(let [metadata_path (str save_dir (str "metadata_" date ".csv"))
  ;      stats (metadata/get-stats metadata_path)]
  ;(println "saved stats to txt file:" (metadata/write-stats2txt stats (str save_dir (str "stats_" date ".txt"))))
  ;(println "saved stats to csv file:" (metadata/create-column-stats-files stats save_dir))))

 )