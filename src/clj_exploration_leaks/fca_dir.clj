(ns clj-exploration-leaks.fca-dir
  (:require [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [clojure.java.io :as io]
            [conexp.fca.contexts :as contexts]
            [conexp.fca.lattices :as lattices]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.set :as set]
            ))

(defn obtain-iceberg-concepts
  "Returns iceberg concepts from a binary context."
  [bin-ctx min-support]
  (let [ice-berg-intents  (lattices/titanic-iceberg-intent-seq bin-ctx 0.8)
        ice-berg-concepts (map #(vector (contexts/attribute-derivation bin-ctx %) %) ice-berg-intents)]
    ice-berg-concepts)
  )

(defn save-iceberg-concepts-to-file
  "Saves iceberg concepts to edn file."
  [ice-berg-concepts file-path file-name]
  (spit (str file-path file-name) (pr-str ice-berg-concepts)) ; writes data to edn file
)

(defn load-iceberg-concepts-from-file
  "Loads iceberg concepts from edn file."
  [file-path file-name]
  (edn/read-string (slurp (str file-path file-name))) ; reads data from edn file
 )


(defn filename-without-extension
  "Returns the filename without extension."
  [filename]
  (let [parts (str/split filename #"\.")]
    (if (> (count parts) 1)
      (str/join "." (butlast parts))
      filename)))

;; context of multiple directories

(defn build-incidence-over-multiple-directories
  "Builds incidence matrix over multiple directories."
  [instances]
  (let [all-topics (->> instances
                        (mapcat (fn [instance]
                                  (mapcat second instance))) ; Collect all topics from all instances
                        set)
        topic->col (zipmap all-topics (range))] ; Map topics to column indices
    ;(let [all-topics (->> instances
    ;                    (mapcat (fn [instance]
    ;                            (map second instance))) ; Collect topics from sub-instances
    ;                  (mapcat identity) ; Flatten nested collections
    ;                  set)                                  ; remove duplicates
    ;    topic->col (zipmap all-topics (range))] ; Map topics to column indices
    ;; Build the incidence matrix
    {:incidence-matrix
     (reduce (fn [matrix instance]
               (let [instance-topics (->> instance
                                          (mapcat second) ; Collect topics from this instance
                                          set)]
                 (conj matrix
                       (vec (map #(if (contains? instance-topics %) 1 0)
                                 all-topics)))))
             []
             instances)
     :topic-map topic->col}))


(defn extract-iceberg-concepts-from-csv-bulk
  "This method extracts and saves concepts from a directory of csv files.
  Each csv file is a binary context.
  A iceberg concept is a pair of intent and extent,
  where each attribute set (intent) belongs to the most frequent ones in the context."
  ([^String path2root-dir ^String concepts-save-path ^Float min-sup]

  ; iterate over all csv files in directory
   (doseq [file (file-seq (io/file path2root-dir))
           :when (.endsWith (.getName file) ".csv")]
     (try
       (let [map-from-zero-one-csv (csv2ctx/zero-one-csv2-map (.getAbsolutePath file))
             ctx (contexts/make-context-from-matrix (:objects map-from-zero-one-csv) (:attributes map-from-zero-one-csv) (:incidence map-from-zero-one-csv))
             ice-berg-concepts (obtain-iceberg-concepts ctx min-sup)
             save-concepts-file-name (str (filename-without-extension (.getName file)) ".edn")
             ]
            (save-iceberg-concepts-to-file ice-berg-concepts concepts-save-path save-concepts-file-name)
         )
     (catch Exception e (str "caught exception on file " (.getName file) ": " (.getMessage e)))
     )
   ))
  ([^String path2root-dir ^String concepts-save-path]
   (extract-iceberg-concepts-from-csv-bulk path2root-dir concepts-save-path 0.9)
   )
)





(println "start")
;(let [path2csv-files "/Users/klara/Downloads/top-per-dir/01_23_25/"
;      concepts-save-path "/Users/klara/Developer/Uni/WiSe2425/clj_exploration_leaks/results/fca-dir-concepts/"]
 ; iterate over all csv files in directory with function
  ;(extract-iceberg-concepts-from-csv-bulk path2csv-files concepts-save-path)

  ; iterate over all csv files in directory manually
  ; (doseq [file (file-seq (io/file path2csv-files))
  ;         :when (.endsWith (.getName file) ".csv")]
  ;   (try
  ;   (let [map-from-zero-one-csv (csv2ctx/zero-one-csv2-map (.getAbsolutePath file))
  ;         ctx (contexts/make-context-from-matrix (:objects map-from-zero-one-csv) (:attributes map-from-zero-one-csv) (:incidence map-from-zero-one-csv))
  ;         ice-berg-concepts (obtain-iceberg-concepts ctx 0.9)
  ;         ]
  ;        (println (.getName file))
  ;        (println "ice-berg-intents of " (.getName file) " from here" ice-berg-concepts)
  ;        (save-iceberg-concepts-to-file ice-berg-concepts concepts-save-path (str (filename-without-extension (.getName file)) ".edn"))
  ;        (println "ice-berg-intents of " (.getName file) " from file" (load-iceberg-concepts-from-file concepts-save-path (str (.getName file) ".edn")))
  ;     )
  ;   (catch Exception e (str "caught exception: " (.getMessage e)))
  ;   )
  ; )
;)


;; Example usage:
 (def data
  [ [ [#{:doc1 :doc2 :doc4} #{:topic_15 :topic_13}]
      [#{:doc3 :doc5} #{:topic_11}] ]
    [ [#{:doc2 :doc6} #{:topic_14 :topic_13}]
      [#{:doc4} #{:topic_12}] ]
    [ [#{:doc_0 :doc_1} #{:topic_55}] ]
    [ [#{:doc_2 :doc_6} #{}] ]
   ])
(def result (build-incidence-over-multiple-directories data))
(def incidence-matrix (:incidence-matrix result))
(def topic-map (:topic-map result))
(println "data" data)
(println "incidence-matrix" incidence-matrix)
(println "topic-map" topic-map)

(defn parse-instance [instance]
  ;; Convert strings to keywords in docs and topics
  (mapv (fn [[docs topics]]
         [(set (map keyword docs)) (set (map keyword topics))])
       instance))

(defn load-instances-from-dir [dir-path]
  ;; Parse all EDN files in the directory and collect instances
  (let [edn-files (->> (file-seq (io/file dir-path))
                       (filter #(.endsWith (.getName %) ".edn")))]
    (reduce (fn [acc file]
              (let [content (edn/read-string (slurp file))]
                (conj acc (parse-instance content))))
            []
            edn-files)))

(let [path2csv-files "/Users/klara/Downloads/top-per-dir/01_23_25/"
      concepts-save-path "/Users/klara/Developer/Uni/WiSe2425/clj_exploration_leaks/results/fca-dir-concepts/"
      data []
      updated-data (into data (load-instances-from-dir concepts-save-path))
      result (build-incidence-over-multiple-directories updated-data)
      incidence-matrix (:incidence-matrix result)
      topic-map (:topic-map result)
      ]
  (println "updated-data" updated-data)
  (println "incidence-matrix" incidence-matrix)
  (println "topic-map" topic-map)
  )

; TODO: sequence of sequences to context
;