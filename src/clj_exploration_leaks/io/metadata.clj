(ns clj-exploration-leaks.io.metadata
  (:require [clj-exploration-leaks.utils.fileSystem :as fileSystem]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
    ;[java-time.api :as jt] ; FIXME: dependency resolution error in project.clj
            ))

;; This file provides methods to extract metadata (file type, file path, file size) from all files in a directory (recursively).

(defn save2csv
  "Saves data to save_path with name optionally specified.
  If out_filename is not specified, it is metadata.csv."
  ([^String save_path ^String out_filename data]
    (fileSystem/exists_or_create (java.io.File. save_path)) ; Create save dir if not existing
    (with-open [writer (io/writer (str save_path "/" out_filename))]
      (csv/write-csv writer data)))

  ([^String save_path data]             ; second declaration with different arity
    (save2csv save_path "metadata.csv" data))
  )


(defn get-parent-dir
  [file-instance]
  (let [parent (.getParentFile file-instance)]
  (if (nil? parent)
    "/"
    (.getName parent)
    )))

(defn find_metadata
  "This functions returns a sequence of maps which contain metadata for all files in the input directory.
  The function also works if the input is a file and not a directory.
  Metadata include file-name, parent directory and file type.
  The size of a directory reflects the space of disk allocated to the directory structure and its metadata,
  not the files it contains."
  [^String target_file_name]
  (let [file-instance (java.io.File. target_file_name)]     ; Cast to file object
    (if (.exists file-instance)                                   ; Check if the input file exists
      (let [metadata (for [file-name (file-seq file-instance)]   ; Construct metadata: sequence of maps
                       ; maps with keys :file_name, :file_path, :parent_dir :len & :type
                       {:file_name (.getName file-name)
                        :file_path (.getAbsolutePath file-name)
                        :parent_dir (get-parent-dir file-name)
                        :len (.length file-name)
                        :type (if (.isDirectory file-name)
                               "dir"                      ; If directory
                               (let [f_extension (last (str/split (.getName file-name) #"\."))] ; If type file: use extension
                                 (if f_extension f_extension "unknown")))})] ; If file has no extension; avoid nil
        (if (seq metadata) ; Ensure there is metadata to process
          metadata
          (println "No files found."))) ; Handle case with no files         ; Return metadata

      (println "File not found!"))))


(defn write-metadata-csv
  "writes the content of row-data of form:
  [{:a 'complete' :b 1 :c 'Friday'}
  {:a 'Started' :b 1 :c 'Monday'}
  {:a 'In Progress' :b 3 :c 'Sunday'}]
  to a .csv file whose name and location is specified with path.
  Is called with content returned by find-metadata.

  Code inspired by: https://stackoverflow.com/questions/18572117/convert-collection-of-hash-maps-to-a-csv-file (08.10.2024)
  "
  ([row-data save_path file_name]
  (fileSystem/exists_or_create (java.io.File. save_path))
  (let [columns [:file_name :len :type :file_path :parent_dir]          ; Vector called columns with 4 keywords
        headers (map name columns)                          ; Name function returns the string version of the keywords of the columns vector
        ; Lambda/ anonymous function: #(.); create function without explicitly naming it
        ; % is placeholder for a row from row-data passed to the lambda function
        ; mapv: applies function (here: row %) to collection (here: columns) -> returns a vector of values associated with columns
        ; mapv returns vector, while map return lazy sequence
        rows (mapv #(mapv % columns) row-data)]             ; Take row and map over the columns:
                                                            ; Retrieve value from row for each column
                                                            ; Result: vector of vectors; inner vector = row
    (with-open [file (io/writer (str save_path "/" file_name))]
      (csv/write-csv file (cons headers rows)))))           ; Cons sets headers as first element in sequence
  ;; declarations with different arity
  ([row-data file_name]
   (write-metadata-csv row-data(str "sample_results/") file_name)) ; TODO: change default path, maybe create automatically date dir
  ([row-data]
   (write-metadata-csv row-data "metadata.csv"))
  )



(defn get-stats
  "Returns a map which contains individual values and their counts, as well as a total count of all entries.
  If no column number is specified, a map containing all column's statistics is returned."
  ;; First arity: Takes `csv-path` and `column-number`
  ([^String csv-path ^Integer column-number]
   (let [file-instance (java.io.File. csv-path)]  ; Cast to file object
     (if (.exists file-instance)                  ; Check if the input file exists
       (with-open [reader (io/reader csv-path)]
         (let [data (doall (csv/read-csv reader)) ; Read and realize CSV data
               nth-column (map #(nth % column-number) (rest data)) ; Extract the nth column
               counts (frequencies nth-column) ; Get frequency count of each unique value
               total-count (count nth-column)]              ; compute total number of entries
           {:counts counts :total-count total-count}))      ; return map
       (println "File not found!"))))

  ;; Second arity: Takes only `csv-path` -> call function on all columns
  ([^String csv-path]
   (let [file-instance (io/file csv-path)]  ; Cast to file object
     (if (.exists file-instance)             ; Check if the input file exists
       (with-open [reader (io/reader csv-path)]
         (let [data (doall (csv/read-csv reader)) ; CSV data
               header (first data)                ; Get the first row (header)
               number-cols (count header)         ; Count the number of columns
               result (mapv (fn [n]
                              (let [stats (get-stats csv-path n)] ; Call `get-stats` for each column
                              {:column (nth header n)   ; Column name from header
                               :counts (:counts stats)
                               :total-count (:total-count stats)
                               })
                              )                             ; function is called with every n value by mapv
                            (range number-cols))] ; Iterate from 0 to (number-cols - 1)
           result))  ; Return the fully evaluated result

       (do
         (println "File not found!")
         nil)))))


(defn write-stats2txt
"Writes column statistics to a text file with indented counts.
The text-file is saved as output-path. Hence, include the filename with .txt extension."
[stats ^String output-path]
(with-open [writer (io/writer output-path)]
  ; :keys deconstructs every map in stats & saves the value of the keys specified (column, counts, ...)
  (doseq [{:keys [column counts total-count]} stats]        ; iterate over collection of maps via doseq
    (.write writer (str column "\n"))            ; Write the column name (no indentation)
    (doseq [[value count] counts]                ; iterate over collection of maps consisting of value, count pairs
      (.write writer (str "  " value ": " count "\n"))) ; Write each value with an indent
    (.write writer "\n")))
  true)                      ; Separate each column's stats

(defn write-stats-to-csv
  "Writes the statistics (value, absolute count, percent occurrence) of a column 'column-name' to a separate CSV file.
  The text-file is saved as output-dir/column-name-stats.csv.
  Is called by create-column-stats-files."
  [^String column-name stats ^String output-dir]
  (let [output-path (str output-dir "/" column-name "-stats.csv")
        ; deconstruct using :keys and save the corresponding values in counts and total-count
        {:keys [counts total-count]} stats]                 ; stats is a single map
    (with-open [writer (io/writer output-path)]
      (csv/write-csv writer [["Value" "Count" "Percentage (%)"]]) ; Header row
      (doseq [[value count] counts]                         ; iterate over all value, count pairs in collection counts
        (let [percentage (* (/ (double count) total-count) 100)] ; calculate percentage of occurrence
           (csv/write-csv writer [[value count (format "%.2f" percentage)]])))))) ; Data rows

(defn create-column-stats-files
  "Generates a unique CSV file with statistics for each column in the input CSV.
  "
  [stats ^String output-dir]
  ; :key deconstructs a map into its values saved in variables called as the corresponding keys
    (doseq [{:keys [column counts total-count]} stats]      ; iterates over all maps in collection stats
      (let [subset-stats {:counts counts, :total-count total-count}] ; subset-stats does not include column name
        (write-stats-to-csv column subset-stats output-dir)))
  true)



;(println (find_metadata "src"))
;(write-metadata-csv (find_metadata "src"))
;(println (get-stats "sample_results/test1510/metadata.csv"))
;(println (write-stats2txt (get-stats "sample_results/test1510/metadata.csv") "sample_results/stats.txt"))
;(println (create-column-stats-files (get-stats "sample_results/test1510/metadata.csv") "sample_results/"))