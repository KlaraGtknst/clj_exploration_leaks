(ns clj-exploration-leaks.utils.fileSystem)

;; this script should provide utilities for handling file creation etc.

(defn exists_or_create
  [^java.io.File dir-instance]
    (when-not (.exists dir-instance)                           ; Check if the output directory exists
      (.mkdirs dir-instance))                                  ; Create the directory (incl. parent directories) if it doesn't
  )


;; Example usage:
;(let [dir (java.io.File. "sample_results/temp/test123")]
;  (exists_or_create dir))