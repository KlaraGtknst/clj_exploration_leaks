(ns clj-exploration-leaks.display-server-results
  (:require [clj-exploration-leaks.io.directory :as dir]
            [clj-exploration-leaks.io.metadata :as metadata]
            [clj-exploration-leaks.io.files :as files]
            [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [conexp.fca.many-valued-contexts :as mv_contexts]
            [clojure.java.io :as io]))


; define directory and date
(def save_dir "results/clj-res-08012025/")
(def date "080125")


; test whether update incidence works via display-bin-ctx

(println "-----------------")
(println "-----------------")


(let [file_path (str save_dir (str "metadata_" date ".csv"))
      ctx_map (csv2ctx/extract-obj-attr-inc file_path)
      bin-ctx-map (csv2ctx/extract-obj-attr-inc-binary file_path)
      old-bin-ctx-map (nth bin-ctx-map 2)
      updated-bin-ctx-map (files/read-edn-file (str save_dir "updated-binary-valued-080125.edn"))
      complete-updated-bin-ctx-map (csv2ctx/insert-updated-ctx-into-ctx-seq bin-ctx-map updated-bin-ctx-map 2)
      ]

  ;(println "Old Binary-valued:")
  ;(csv2ctx/display-bin-ctx old-bin-ctx-map)
  ;(println "-----------------")
  ;(println "updated binary-valued:")
  ;(csv2ctx/display-bin-ctx updated-bin-ctx-map)
  (println "-----------------")
  (println "second vector elem:" (:attributes (nth (vec bin-ctx-map) 2)))
  (println "attributes of updated obj" (:attributes updated-bin-ctx-map))
  (println "first: old-bin-ctx-map (2nd elem of bin-ctx-map); after: updated (old with changed second incidence)")
  (println old-bin-ctx-map)
  (println "-----------------")
  (println "Insert updated binary-valued:")
  (csv2ctx/display-bin-ctx complete-updated-bin-ctx-map)
  ;(println "-----------------")
  ;(println "test:" updated-bin-ctx-map)
  ;(println "test:" (#(assoc % :incidence (:incidence updated-bin-ctx-map)) (nth (vec bin-ctx-map) 2)))

  )