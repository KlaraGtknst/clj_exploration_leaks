(ns clj-exploration-leaks.fca-dir
  (:require [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [clojure.data.csv :as csv]
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
  "Saves iceberg concepts to edn file.
  If the file name contains 'translated', the file is saved in a subdirectory 'translated'."
  [ice-berg-concepts file-path file-name]
  (let [target-path (if (.contains file-name "translated")
                        (str file-path "translated/")
                        file-path)]
  (spit (str target-path file-name) (pr-str ice-berg-concepts))) ; writes data to edn file
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
  "Builds an incidence matrix over multiple directories, where each directory contains a set of instances.
  Each instance consists of a collection of topic pairs, and the function generates a matrix where rows
  correspond to instances and columns correspond to topics. The matrix entry at a given row and column
  is 1 if the instance contains the topic, otherwise 0.

  Parameters:
  - instances: A collection of directories, where each directory is a collection of instances.
    Each instance is a collection of topic pairs, with the first element typically being an identifier
    and the second element being a collection of topics.

  Returns:
  A map containing:
  - :incidence-matrix: A list of vectors representing the incidence matrix.
  - :topic-map: A map from topics to their corresponding column indices in the incidence matrix."
  [instances]
  (let [all-topics (->> instances
                        (mapcat (fn [instance]
                                  ;; For each instance, collect the topics (second element of pairs).
                                  (mapcat second instance)))
                        ;; Collect all unique topics into a set.
                        set)
        ;; Create a map of each topic to its corresponding column index.
        topic->col (zipmap all-topics (range))]
    {:incidence-matrix
     (reduce (fn [matrix instance]
               (let [instance-topics (->> instance
                                          ;; For this instance, collect all topics (second element of pairs).
                                          (mapcat second)
                                          ;; Convert to a set for efficient lookup.
                                          set)]
                 (conj matrix
                       ;; Create a row where each entry is 1 if the topic is in the instance-topics, otherwise 0.
                       (vec (map #(if (contains? instance-topics %) 1 0)
                                 all-topics)))))
             ;; Start with an empty matrix (initial value for reduce).
             []
             ;; Process each instance.
             instances)
     ;; Include the topic-to-column mapping in the result.
     :topic-map topic->col}))



(defn extract-iceberg-concepts-from-csv-bulk
  "This method extracts and saves concepts from a directory of csv files.
  Each csv file is a binary context.
  A iceberg concept is a pair of intent and extent,
  where each attribute set (intent) belongs to the most frequent ones in the context."
  ([^String path2root-dir ^String concepts-save-path ^Float min-sup]

  ; iterate over all csv files in directory
   (doseq [file (file-seq (io/file path2root-dir))
           :when (and (.endsWith (.getName file) ".csv")
                      (.contains (.getName file) "thres")   ; only thresholded incidence matrices
                      (not (.contains (.getName file) "term")) ; exclude term-topic incidence matrices
                      (not (.contains (.getName file) "translated")))] ; exclude translated files (doc has title name)
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
; (def data
;  [ [ [#{:doc1 :doc2 :doc4} #{:topic_15 :topic_13}]
;      [#{:doc3 :doc5} #{:topic_11}] ]
;    [ [#{:doc2 :doc6} #{:topic_14 :topic_13}]
;      [#{:doc4} #{:topic_12}] ]
;    [ [#{:doc_0 :doc_1} #{:topic_55}] ]
;    [ [#{:doc_2 :doc_6} #{}] ]
;   ])
;(def result (build-incidence-over-multiple-directories data))
;(def incidence-matrix (:incidence-matrix result))
;(def topic-map (:topic-map result))
;(println "data" data)
;(println "incidence-matrix" incidence-matrix)
;(println "topic-map" topic-map)

(defn parse-instance [instance]
  ;; Convert strings to keywords in docs and topics
  (mapv (fn [[docs topics]]
         [(set (map keyword docs)) (set (map keyword topics))])
       instance))

(defn load-instances-from-dir
  "Loads instances from all EDN files in the specified directory.

  This function reads each EDN file in the directory, parses its content, and converts it into an
  instance structure using the `parse-instance` function. Additionally, it collects the filenames
  (excluding the '.edn' file extension) corresponding to the instances.

  Parameters:
  - dir-path: A string representing the path to the directory containing EDN files.

  Returns:
  A map containing:
  - :instances: A vector of parsed instances, where each instance is the result of `parse-instance`.
  - :filenames: A vector of filenames (without the '.edn' extension), ordered to correspond to the instances."
  [dir-path]
  ;; Get all files in the directory with a ".edn" extension, but not in subdirectories
  (let [edn-files (->> (.listFiles (io/file dir-path))
                       ;; Filter only the files ending with ".edn" & are files
                        (filter #(and (.isFile %) (.endsWith (.getName %) ".edn"))))]
    ;; Process the files and return the parsed instances along with their filenames
    (reduce (fn [acc file]
              (let [content (edn/read-string (slurp file))  ; Read and parse EDN content
                    instance (parse-instance content)       ; Parse the content into an instance
                    filename (-> (.getName file)            ; Extract the filename
                                 (clojure.string/replace #"\.edn$" ""))] ; Remove the '.edn' extension
                ;; Add the parsed instance and filename to the accumulators
                {:instances (conj (:instances acc) instance)
                 :filenames (conj (:filenames acc) filename)}))
            ;; Initialize the accumulator with empty vectors
            {:instances [] :filenames []}
            edn-files)))

(defn write-across-dir-topic-incidence-csv
  "Creates a CSV file from directory names, incidence matrix, and topic map.

  Parameters:
  - dir-names: A vector of directory names (e.g., [\"dir1\", \"dir3\", \"dir2\", \"dir15\"]).
  - incidence-matrix: A vector of vectors representing incidences between directories and topics
    (e.g., [[1 0] [0 0] [1 0] [0 0]]).
  - topic-map: A map from topic keywords to column indices (e.g., {:topic_55 0, :topic_231 1}).
  - output-path: A string representing the file path where the CSV will be written.

  Output:
  - Writes the CSV file to the specified output path."
  [dir-names incidence-matrix topic-map output-path]
  ;; Generate the CSV data
  (let [topics (->> topic-map
                    (sort-by val)         ; Sort topics by their column indices
                    (map key)             ; Extract topic keywords
                    (map name))           ; Convert keywords to strings
        ;; Create rows with directory names and their corresponding incidences
        rows (map (fn [dir incidences]
                    (cons dir incidences))
                  dir-names
                  incidence-matrix)
        ;; Combine the header row (topics) with the data rows
        csv-data (cons (cons "Directory" topics) rows)]
    ;; Write the CSV data to the specified output path
    (with-open [writer (io/writer output-path)]
      (csv/write-csv writer csv-data))))

(defn truncate-before-underscore
  "Takes a list of strings and returns the same list with each string truncated
   at the first underscore, if present. If no underscore is found, the string remains unchanged.

  Parameters:
  - strings: A list of strings to process.

  Returns:
  - A list of strings truncated as described."
  [strings]
  (map (fn [s]
         (let [underscore-index (.indexOf s "_")]
           (if (>= underscore-index 0)  ; Check if underscore is present
             (subs s 0 underscore-index) ; Truncate at underscore
             s)))                        ; Return the whole string if no underscore
       strings))

(defn subdir-topic-inc2ctx-csv
  "This is the function that combines the previous functions to create a context from a directory of incidences of subdirectories and topics.
  The output-save-path is the path where the context will be saved as a csv file (should end with /, thus, not containing filename."
  [input-path concepts-save-path output-save-path]
  (extract-iceberg-concepts-from-csv-bulk input-path concepts-save-path)
  (let [data []
        seq-instances-map (load-instances-from-dir concepts-save-path)
        updated-data (into data (:instances seq-instances-map))
        list-of-file-dir-names (:filenames seq-instances-map)
        result (build-incidence-over-multiple-directories updated-data)
        incidence-matrix (:incidence-matrix result)
        topic-map (:topic-map result)
      ]
  (write-across-dir-topic-incidence-csv (truncate-before-underscore list-of-file-dir-names) incidence-matrix topic-map (str output-save-path "across-dir-incidence-matrix.csv"))
  )
  )



;(let [path2csv-files "/Users/klara/Downloads/top-per-dir/01_23_25/"
;      concepts-save-path "/Users/klara/Developer/Uni/WiSe2425/clj_exploration_leaks/results/fca-dir-concepts/"
;      data []
;      seq-instances-map (load-instances-from-dir concepts-save-path)
;      updated-data (into data (:instances seq-instances-map))
;      list-of-file-dir-names (:filenames seq-instances-map)
;      result (build-incidence-over-multiple-directories updated-data)
;      incidence-matrix (:incidence-matrix result)
;      topic-map (:topic-map result)
;      ]
;  (println "filenames" list-of-file-dir-names)
;  (println "updated-data" updated-data)
;  (println "incidence-matrix" incidence-matrix)
;  (println "topic-map" topic-map)
;  (write-across-dir-topic-incidence-csv (truncate-before-underscore list-of-file-dir-names) incidence-matrix topic-map "/Users/klara/Developer/Uni/WiSe2425/clj_exploration_leaks/results/fca-dir-concepts/across-dir/across-dir-incidence-matrix.csv")
;  (println "all done")
;  (subdir-topic-inc2ctx-csv concepts-save-path "/Users/klara/Developer/Uni/WiSe2425/clj_exploration_leaks/results/fca-dir-concepts/across-dir/")
;  )

;(let [path2csv-files "/Users/klara/Downloads/top-per-dir-24-01-25/"
;      concepts-save-path "/Users/klara/Developer/Uni/WiSe2425/clj_exploration_leaks/results/fca-dir-concepts/"
;      output-save-path "/Users/klara/Developer/Uni/WiSe2425/clj_exploration_leaks/results/fca-dir-concepts/across-dir/"
;      across-dir-ctx (csv2ctx/zero-one-csv2-map (str output-save-path "across-dir-incidence-matrix.csv"))
;      objects (:objects across-dir-ctx)
;      attributes (:attributes across-dir-ctx)
;      incidence (:incidence across-dir-ctx)
;      ctx (contexts/make-context-from-matrix objects attributes incidence)
;      ]
;  ;; create csv file containing incidence matrix of subdirectories and topics
;    (subdir-topic-inc2ctx-csv path2csv-files concepts-save-path output-save-path)

  ;; display context
  ;  (println "across-dir-ctx" across-dir-ctx)
  ;  (csv2ctx/display-bin-ctx across-dir-ctx)
  ;  (println "objects" objects)
  ;  (println "attributes" attributes)
  ;  (println "incidence" incidence)
  ;  (csv2ctx/compute-titanic-iceberg-lattice ctx)
  ;)
