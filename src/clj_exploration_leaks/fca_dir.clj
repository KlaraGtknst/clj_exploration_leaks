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





(println "start")
;(let [path2csv-files "/Users/klara/Developer/Uni/WiSe2425/text_topic/results/incidences/"
;      concepts-save-path "/Users/klara/Developer/Uni/WiSe2425/clj_exploration_leaks/results/"]
;  ; iterate over all csv files in directory
;   (doseq [file (file-seq (io/file path2csv-files))
;           :when (.endsWith (.getName file) ".csv")]
;     (try
;     (let [map-from-zero-one-csv (csv2ctx/zero-one-csv2-map (.getAbsolutePath file))
;           ctx (contexts/make-context-from-matrix (:objects map-from-zero-one-csv) (:attributes map-from-zero-one-csv) (:incidence map-from-zero-one-csv))
;           ice-berg-concepts (obtain-iceberg-concepts ctx 0.1)
;           ]
;          (println (.getName file))
;          (println "ice-berg-intents of " (.getName file) " from here" ice-berg-concepts)
;          (save-iceberg-concepts-to-file ice-berg-concepts concepts-save-path (str (filename-without-extension (.getName file)) ".edn"))
;          (println "ice-berg-intents of " (.getName file) " from file" (load-iceberg-concepts-from-file concepts-save-path (str (.getName file) ".edn")))
;       )
;     (catch Exception e (str "caught exception: " (.getMessage e)))
;     )
;   )
;)

;; Example usage:
 (def data
  [ [ [#{:doc1 :doc2 :doc4} #{:topic_15 :topic_13}]
      [#{:doc3 :doc5} #{:topic_11}] ]
    [ [#{:doc2 :doc6} #{:topic_14 :topic_13}]
      [#{:doc4} #{:topic_12}] ] ])
(def result (build-incidence-over-multiple-directories data))
(def incidence-matrix (:incidence-matrix result))
(def topic-map (:topic-map result))
(println incidence-matrix)
(println topic-map)