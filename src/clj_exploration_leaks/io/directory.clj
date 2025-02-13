(ns clj-exploration-leaks.io.directory
  (:require [clj-exploration-leaks.utils.fileSystem :as fileSystem]))

;; This file should provide methods that create/ display/ save a file tree

;; Define structures
(defstruct file :file_name)   ; File with field 'file_name'
(defstruct dir :dir_name :contents)   ; Dir with fields: 'dir_name' and 'contents'


(defn file-tree
  "returns the file tree as a nested structure construct using structures defined above.
  If input is a directory the method calls itself recursively.
  The input parameter is a java.io.File."
  [^java.io.File input_file]   ; Args file of type File from Java standard library; type hint improves performance
  (if (.isDirectory input_file)    ; Check whether file is directory
    ;; If yes:
    ;; Create dir structure with file in file field
    ;; & create a vector consisting of recursive function calls of all files in that directory
    (struct dir input_file (vec (map file-tree (.listFiles input_file))))
    ;; If no: Create file structure
    (struct file input_file)))


(defn print-tree
  "Prints the directory in an indentation tree structure to the console.
  :: tree :: A directory tree structure derived from [[file-tree]].
  :: level :: Integer; defines level of indentation; starts with 0."
  [tree level]
  (let [indent (apply str (repeat level "  "))]  ; Create indentation
    (cond
      ;; It is not possible to check if tree is instance of class struct dir: https://groups.google.com/g/clojure/c/peC6ZIDQSCk#3fbd539b807a506d
      (every? #{:dir_name :contents} (keys tree))   ; Check if it's a directory struct type by checking its keys
      (do
        (println indent "DIR:" (.getName (:dir_name tree)))  ; Directory name
        (doseq [child (:contents tree)]               ; Iterate over contents
          (print-tree child (inc level)))            ; Recur with increased indentation
        )
      (every? #{:file_name} (keys tree))        ; File
      (println indent "FILE:" (.getName (:file_name tree))) ; File name
      :else (println indent "UNKNOWN TYPE"))))      ; Handle unexpected cases

(defn save-file-tree
  "Creates a file called 'file-tree.txt' at 'save_path' location.
  If 'save_path' does not exist, it is created.
  Content of file is the directory structure/ tree derived from [[print-tree]].
  Optional: Specify file name."
  ([^String file ^String save_path ^String out_file_name]
   (let [file-instance (java.io.File. file)                 ; Cast to file object
         output-dir (java.io.File. save_path)               ; Directory for output
         output-file (java.io.File. (str save_path "/" out_file_name))] ; Output file
     (if (.exists file-instance)                            ; Check if the input file exists
       (do
         (fileSystem/exists_or_create output-dir)           ; Create the directory (incl. parent directories) if it doesn't
         (binding [*out* (clojure.java.io/writer output-file)] ; Bind output to the file
           (print-tree (file-tree file-instance) 0))        ; Start printing from level 0
         )
       (println "File not found!"))))
  ([^String file ^String save_path]                         ; Second declaration with different arity
   (save-file-tree file save_path "file-tree.txt"))
  )


;; Example usage
;; Create a File instance and call file-tree
;(let [file-instance (java.io.File. "src")]                  ; cast to file object
;  (if (.exists file-instance)                               ; Check if the file exists
;    (print-tree (file-tree file-instance) 0)                ; Start printing from level 0
;    (println "File not found!")))
;(save-file-tree "src" "sample_results/temp/test/12")
;(save-file-tree "src" "sample_results/test1510" "file-tree.txt")