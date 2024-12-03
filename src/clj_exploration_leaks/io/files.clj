(ns clj-exploration-leaks.io.files
  (:require [clojure.java.io :as io]))

;; this file provides functions to check whether files exit, read file contents and write to files
;; the code is inspired by https://www.tutorialspoint.com/clojure/clojure_file_io.htm (07.10.2024)

(defn readFile
  "Returns input of .txt file as string and prints it to IO"
  [filename]
  (let [content (slurp (io/resource filename))]              ;; let = local binding, def = global variable -> change brackets a bit & omit []
  (println content)                                         ;; prints content to IO
  content))                                                 ;; returns content

(def x (readFile "example.txt"))


(defn writeFile
  "Writes content of Args into a file called 'filename'."
  [content filename]                                        ;; separate multiple arguments by space
  (spit filename content))

(when x                                                     ;; avoid null pointer exception
 (writeFile x "newFile.txt"))


(defn fileExists
  "Returns a Boolean. True if file 'filename' exits, else false."
  [filename]
  (let [exists (.exists (io/file filename))]
    exists))

(println "the file newFile.txt exists: " (fileExists "newFile.txt"))
(println "the file test.txt exists: " (fileExists "test.txt"))
