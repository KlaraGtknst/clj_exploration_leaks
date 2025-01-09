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
      upt (update (vec bin-ctx-map) 2 #(assoc % :incidence (:incidence updated-bin-ctx-map))) ; fixme: Doesn't work, empty context
      ]

  (println "Old Binary-valued:")
  (csv2ctx/display-bin-ctx old-bin-ctx-map)
  ;(println "-----------------")
  ;(println "updated binary-valued:")
  ;(csv2ctx/display-bin-ctx updated-bin-ctx-map)
  (println "-----------------")
  (println "Insert updated binary-valued:")
  (csv2ctx/display-bin-ctx upt)

  )