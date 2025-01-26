(ns clj-exploration-leaks.fca-dir
  (:require [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [conexp.fca.contexts :as contexts]
            [conexp.fca.lattices :as lattices]
            [clojure.edn :as edn]
            [clojure.string :as str]
            )
  (:import (java.text SimpleDateFormat)))

(defn _obtain-iceberg-concepts
  "Returns iceberg concepts from a binary context.

  Parameters:
  - bin-ctx: A binary context.
  - min-support: The minimum support threshold for iceberg concepts.

  Returns:
  A sequence of iceberg concepts, where each concept is a pair of intent and extent."
  [bin-ctx ^Float min-support]
  (let [ice-berg-intents  (lattices/titanic-iceberg-intent-seq bin-ctx min-support)
        ice-berg-concepts (map #(vector (contexts/attribute-derivation bin-ctx %) %) ice-berg-intents)]
    ice-berg-concepts)
  )

(defn _save-iceberg-concepts-to-file
  "Saves iceberg concepts to edn file.
  If the file name contains 'translated', the file is saved in a subdirectory 'translated'.

  Parameters:
  - ice-berg-concepts: A sequence of iceberg concepts. They can be obtained by the function obtain-iceberg-concepts.
  - file-path: The path to the directory where the file will be saved.
  - file-name: The name of the file."
  [ice-berg-concepts ^String file-path ^String file-name]
  (let [target-path (if (.contains file-name "translated")
                        (str file-path "translated/")
                        file-path)]
  (spit (str target-path file-name) (pr-str ice-berg-concepts))) ; writes data to edn file
)
         
(defn _load-iceberg-concepts-from-file
  "Loads iceberg concepts from edn file.
  The file is created by the function save-iceberg-concepts-to-file.

  Parameters:
  - file-path: The path to the directory where the file is saved.
  - file-name: The name of the file.

  Returns:
  A sequence of iceberg concepts."
  [^String file-path ^String file-name]
  (edn/read-string (slurp (str file-path file-name))) ; reads data from edn file
 )


(defn _filename-without-extension
  "Returns the filename without extension.

  Parameters:
  - filename: The name of the file.

  Returns:
  The filename without extension.
  "
  [^String filename]
  (let [parts (str/split filename #"\.")]
    (if (> (count parts) 1)
      (str/join "." (butlast parts))
      filename)))

;; context of multiple directories

(defn _build-incidence-over-multiple-directories
  "Builds an incidence matrix over multiple directories, where each directory contains a set of instances
  (objects: documents & attributes: topics).
  Each instance consists of a collection of topic pairs (document-topic), and the function generates a matrix where rows
  correspond to instances and columns correspond to topics. The matrix entry at a given row and column
  is 1 if the instance contains the topic, otherwise 0.

  Parameters:
  - instances: A collection of directories, where each directory is a collection of instances.
    Each instance is a collection of topic pairs, with the first element typically being an identifier
    and the second element being a collection of topics. Can be obtained from the function `load-instances-from-dir`.

  Returns:
  A map containing:
  - :incidence-matrix: A list of vectors representing the incidence matrix between directories and topics.
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


(defn _extract-date-from-filename
  "Extracts the date from the filename and parses it into a Date object.
   Assumes the date format is '_MM_dd_yy' (e.g., '_01_26_25').

   Parameters:
   - filename: The name of the file.

   Returns:
   A Date object representing the extracted date, or nil if no date is found."
  [^String filename]
  (let [date-regex #"_\d{2}_\d{2}_\d{2}"
        date-str (re-find date-regex filename)]
    (when date-str
      (let [date-format (SimpleDateFormat. "_MM_dd_yy")]
        (.parse date-format date-str)))))

(defn _extract-iceberg-concepts-from-csv-bulk
  "This method extracts and saves concepts from a directory of csv files.
  Each csv file is a binary context.
  An iceberg concept is a pair of intent and extent,
  where each attribute set (intent) belongs to the most frequent ones in the context.

  Parameters:
  - path2root-dir: The path to the directory containing the csv files.
  - concepts-save-path: The path to the directory where the iceberg concepts will be saved.
  - min-sup: Minimal support threshold for iceberg concepts.

  Returns: -
  "
  ([^String path2root-dir ^String concepts-save-path ^Float min-sup]

   ; Collect and deduplicate files by keeping only the newest version based on filename date
   (let [files (->> (file-seq (io/file path2root-dir))
                    (filter #(and (.endsWith (.getName %) ".csv")
                                  (.contains (.getName %) "thres")  ; only thresholded incidence matrices
                                  (not (.contains (.getName %) "term"))  ; exclude term-topic incidence matrices
                                  (not (.contains (.getName %) "translated")))) ; exclude translated files
                    (group-by #(-> (.getName %)  ; Group files by their base name (without date)
                                   (str/replace #"_\d{2}_\d{2}_\d{2}\.csv" "")
                                   (str/lower-case)))
                    (map (fn [[_ files]]
                           (apply max-key #(or (some-> (_extract-date-from-filename (.getName %))
                                                       .getTime)  ; Convert Date to milliseconds
                                               Long/MIN_VALUE)  ; Fallback if no date is found
                                        files))))]
     ; Process only the newest files
     (doseq [file files]
       (try
         (let [map-from-zero-one-csv (csv2ctx/zero-one-csv2-map (.getAbsolutePath file))
               ctx (contexts/make-context-from-matrix (:objects map-from-zero-one-csv)
                                                      (:attributes map-from-zero-one-csv)
                                                      (:incidence map-from-zero-one-csv))
               ice-berg-concepts (_obtain-iceberg-concepts ctx min-sup)
               save-concepts-file-name (str (_filename-without-extension (.getName file)) ".edn")]
           (_save-iceberg-concepts-to-file ice-berg-concepts concepts-save-path save-concepts-file-name))
         (catch Exception e
           (str "caught exception on file " (.getName file) ": " (.getMessage e))))))
   )
  ([^String path2root-dir ^String concepts-save-path]
   (_extract-iceberg-concepts-from-csv-bulk path2root-dir concepts-save-path 0.9)))

(defn _parse-instance
  "Parses an instance from a map containing document-topic concepts.
  Converts the document ID and topic ID strings to keywords."
  [instance]
  (mapv (fn [[docs topics]]
         [(set (map keyword docs)) (set (map keyword topics))])
       instance))

(defn _load-instances-from-dir
  "Loads instances from all EDN files in the specified directory, keeping only the newest versions.
  Each EDN file corresponds to a directory and contains a document-topic concepts.
  Filters EDN files based on dates in their filenames, ensuring only the latest file for each base name
  is processed. Parses the content and converts it into an instance structure.

  Parameters:
  - dir-path: A string representing the path to the directory containing EDN files.

  Returns:
  A map containing:
  - :instances: A vector of parsed instances, where each instance is the result of `_parse-instance`,
    i.e. document-topic concepts.
  - :filenames: A vector of filenames (without the '.edn' extension), ordered to correspond to the instances."
  [^String dir-path]
  (let [edn-files (->> (.listFiles (io/file dir-path))      ; only files, not directories
                       (filter #(and (.isFile %) (.endsWith (.getName %) ".edn")))) ; Filter EDN files
        newest-files (->> edn-files
                          (group-by #(-> (.getName %)       ; filter via filename
                                         (clojure.string/replace #"_\d{2}_\d{2}_\d{2}\.edn$" "") ; omit date in filename
                                         (clojure.string/lower-case))) ; Group by base filename
                          (map (fn [[_ files]]
                                 (apply max-key #(or (some-> (_extract-date-from-filename (.getName %))
                                                             .getTime)
                                                     Long/MIN_VALUE)
                                          files))))]        ; Keep only the newest files
    ;; Process the newest files
    (reduce (fn [acc file]
              (let [content (edn/read-string (slurp file))  ; Read and parse EDN content
                    instance (_parse-instance content)       ; Parse the content into an instance
                    filename (-> (.getName file)            ; Extract the filename
                                 (clojure.string/replace #"\.edn$" ""))] ; Remove the '.edn' extension
                {:instances (conj (:instances acc) instance)
                 :filenames (conj (:filenames acc) filename)}))
            {:instances [] :filenames []}                   ; ensure vectors as inner structure
            newest-files)))

(defn _write-across-dir-topic-incidence-csv
  "Creates a CSV file from directory names, incidence matrix, and topic map.

  Parameters:
  - dir-names: A vector of directory names (e.g., [\"dir1\", \"dir3\", \"dir2\", \"dir15\"]).
  - incidence-matrix: A vector of vectors representing incidences between directories and topics
    (e.g., [[1 0] [0 0] [1 0] [0 0]]).
  - topic-map: A map from topic keywords to column indices (e.g., {:topic_55 0, :topic_231 1}).
  - output-path: A string representing the file path where the CSV will be written.

  Output:
  - Writes the CSV file to the specified output path."
  [dir-names incidence-matrix topic-map ^String output-path]
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

(defn _truncate-before-underscore
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
  The output-save-path is the path where the context will be saved as a csv file (should end with /, thus, not containing filename.

  Parameters:
  - input-path: The path to the directory containing the incidence matrices.
  - concepts-save-path: The path to the directory where the iceberg concepts will be saved.
  - output-save-path: The path to the directory where the dir-topic context across the directories will be saved as a csv file."
  [^String input-path ^String concepts-save-path ^String output-save-path]
  (_extract-iceberg-concepts-from-csv-bulk input-path concepts-save-path)
  (let [data []
        seq-instances-map (_load-instances-from-dir concepts-save-path)
        updated-data (into data (:instances seq-instances-map))
        list-of-file-dir-names (:filenames seq-instances-map)
        result (_build-incidence-over-multiple-directories updated-data)
        incidence-matrix (:incidence-matrix result)
        topic-map (:topic-map result)
      ]
  (_write-across-dir-topic-incidence-csv (_truncate-before-underscore list-of-file-dir-names) incidence-matrix topic-map (str output-save-path "across-dir-incidence-matrix.csv"))
  )
  )
