(ns clj-exploration-leaks.io.csv2ctx
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [conexp.fca.contexts :as contexts]
            [conexp.fca.lattices :as lattices]
            [conexp.gui.draw :as draw]
            ))

;; this file provides methods to work with contexts
;; Functionalities include:
;; - casting a (zero-one) csv file to a context object
;; - display context using the library conexp-clj
;; - compute (iceberg) concept lattice
;; - update incidence matrix based on attribute and objects (subsets)

(defn extract-obj-attr-inc
  "Extracts the header (attributes), first column (objects) and other rows (incidence)
  from a CSV file at the given filepath."
  [^String filepath]
  (let [file-instance (io/file filepath)]  ; Cast to file object
    (if (.exists file-instance)               ; Check if the input file exists
      (with-open [reader (io/reader filepath)]
        (let [data (doall (csv/read-csv reader))]
          (let [header (first data)                     ; Get the header
                first-column (map first (rest data))   ; Extract the first column
                other-rows (apply concat (rest (map vec data)))]  ; Flatten remaining rows; rest returns all but first row
            {:objects first-column
             :attributes header
             :incidence other-rows})))  ; Return as a map

      (do
        (println "File not found!")
        nil))))  ; Return nil if the file is not found


(defn extract-obj-attr-inc-binary
  "Extracts values of one column (attributes) and first column (objects).
  Returns also a matrix which encodes whether attribute is present for an object (binary).
  If no column number is specified, a clojure.lang.LazySeq of all column results is returned"
  ([^String filepath ^Integer column-number]
   (if (not (> column-number 0))
     (do
       (println "Column index should be greater than zero, because first column contains unique objects!")
       nil)
  (let [file-instance (io/file filepath)]  ; Cast to file object
    (if (.exists file-instance)               ; Check if the input file exists
      (with-open [reader (io/reader filepath)]
        (let [data (doall (csv/read-csv reader))]
          (let [n column-number
                first-column (map first (rest data))   ; Extract the first column
                ;; Extract the nth column (attributes)
                nth-column (map #(nth % n) (rest data))    ; rest returns all but first row
                                                            ; map applies function #(.) to collection (rest data)
                                                            ; # defines anonymous function
                                                            ; % is a placeholder for the current element, i.e. row from collection
                                                            ; nth returns item at n-th index of current row
                unique-attributes (distinct nth-column) ; Get unique attributes
                ;; Create object (first element) - attribute (n-th element) pairs
                obj-attr-pairs (map #(vector (first %) (nth % n)) (rest data)) ; maps anonymous function to each element of rest data
                                                                              ; vector creates vector of object and attribute
                incidence-matrix (for [obj first-column]
                                   (map (fn [attr]          ; anonymous function with input attribute
                                          ; if attribute belongs to object, i.e. an entry exits in obj-attr-pairs: 1
                                          ; some stops if anonymous function returns true or evaluated all elements
                                          (if (some #(= [obj attr] %) obj-attr-pairs) 1 0))
                                        unique-attributes))]; collection of attributes, whose elements are mapped to function values
            {:objects first-column
             :attributes unique-attributes
             :incidence (apply concat incidence-matrix) })))  ; Return as a map

      (do
        (println "File not found!")
        nil)))))  ; Return nil if the file is not found

  ([^String filepath]
   (let [file-instance (io/file filepath)]  ; Cast to file object
     (if (.exists file-instance)               ; Check if the input file exists
       (with-open [reader (io/reader filepath)]
         (let [data (csv/read-csv reader)
               header (first data)          ; Get the first row (header)
               number-cols (count header)  ; Count the number of columns in the header
              result (map (fn [n]
                  (extract-obj-attr-inc-binary filepath n)) ; Call with different `n`
                (range 1 number-cols))] ; Iterate from 1 to (number-cols - 1) -> the highest possible index is accessed
           (doall result)                                   ; ensure result is returned
           ))

       (do
         (println "File not found!")
         nil)))  ;; Return nil if the file is not found
))


(defn display-bin-ctx
  "Method displays binary contexts in form of a cross table.
  Functionality used from conexp.fca.contexts.
  If input contains multiple binary contexts, it should be of type clojure.lang.LazySeq.
  If input contains only one binary context, it should be of type clojure.lang.PersistentArrayMap."
  ([bin-ctxs-seq]
   (if (or (instance? clojure.lang.LazySeq bin-ctxs-seq) (instance? clojure.lang.PersistentList bin-ctxs-seq))
     ;; bin-ctxs-seq is a lazy sequence of binary contexts
     (doseq [bin-ctx bin-ctxs-seq]
       (display-bin-ctx bin-ctx)))

    ;; bin-ctxs-seq is only one binary context
     (let [bin-ctx-res (contexts/make-context-from-matrix (:objects bin-ctxs-seq) (:attributes bin-ctxs-seq) (:incidence bin-ctxs-seq))]
        (println bin-ctx-res))))


(defn obtain-bin-ctx
  "Method returns (not displays) binary contexts.
  Functionality used from conexp.fca.contexts.
  If input contains multiple binary contexts, it should be of type clojure.lang.LazySeq.
  If input contains only one binary context, it should be of type clojure.lang.PersistentArrayMap."
  ([bin-ctxs-seq]
   (if (instance? clojure.lang.LazySeq bin-ctxs-seq)
     ;; bin-ctxs-seq is a lazy sequence of binary contexts
     (doseq [bin-ctx bin-ctxs-seq]
       (display-bin-ctx bin-ctx)))

   ;; bin-ctxs-seq is only one binary context
   (let [bin-ctx-res (contexts/make-context-from-matrix (:objects bin-ctxs-seq) (:attributes bin-ctxs-seq) (:incidence bin-ctxs-seq))]
     bin-ctx-res)))

(defn update-incidence
  "Updates incidence matrix based on attributes and objects.
  If an attribute is included in another attribute, the incidence matrix is updated, such that the object has a 1 not
  only for the child (i.e. longer instance), but also the parent (i.e. shorter instance).
  The incidence matrix is a vector of binary values.
  The data is a map and must have the fields :attributes, :incidence (and :objects)."
  [data]
  (let [attributes (:attributes data)
        incidence (vec (:incidence data))
        num-objects (count (:objects data))
        includes? (fn [string prefix]
                    (and (not= prefix string) (.startsWith string prefix))) ; true if parent = start of child; false otherwise.
        updated-incidence                                   ; updated incidence matrix as defined below
        (reduce
         (fn [incidence [p parent]]
           (reduce
             (fn [incidence [c child]]
               (reduce                                      ; use reduce, not for (creates lazy sequence)
               (fn [incidence obj-num]
                 ;; index of child/ parent in incidence matrix, depends on object number
                 (let [child-index (+ (* num-objects obj-num) c)
                       parent-index (+ (* num-objects obj-num) p)]
                   (if (and (includes? child parent)
                            (= 1 (nth incidence child-index)))
                     (assoc incidence parent-index 1)
                     incidence)))
               incidence
               (range num-objects))
               )
            incidence
            (map-indexed (fn [p v] [p v]) attributes)))             ; returns index and value of attributes as a vector

         incidence                                          ; first input parameter for anonymous function fn
         (map-indexed (fn [c v] [c v]) attributes))]                  ; second input parameter for anonymous function fn
    (assoc data :incidence updated-incidence)))             ; return updated data map


(defn insert-updated-ctx-into-ctx-seq
  "Updates the incidence matrix of a binary context in a sequence of binary contexts.
  The new incidence matrix is inserted at the given position.
  The data is a map and must have the field :incidence (, :attributes, :objects)."
  [bin-ctxs-seq updated-bin-ctx ^Integer position]
  (into '() (assoc (vec bin-ctxs-seq)
                position (assoc (nth bin-ctxs-seq position) :incidence (:incidence updated-bin-ctx)))))

(defn display-lattice
  "Method displays lattice by first printing its atoms and displaying it afterward as a formal context using another application."
  [lattice]
  (println (lattices/lattice-atoms lattice))
  (draw/draw-lattice lattice)
  lattice)

(defn compute-concept-lattice
  "Computes the concept lattice from a binary context.
  Instead of returning the lattice, it is displayed."
  [bin-ctx]
  (let [lattice (lattices/concept-lattice bin-ctx)]
    (display-lattice lattice)))

(defn compute-titanic-iceberg-lattice
  "Computes the iceberg lattice from a binary context.
  Instead of returning the lattice, it is displayed.
  The default value for min-support is 0.1.
  "
  [bin-ctx]
  ;; min-support = relative portion of objects of derivative of intent in total set of objects
  (display-lattice (lattices/iceberg-lattice bin-ctx 0.1))  ; paper: 0.03
  )

(defn zero-one-csv2-map
  "Extracts attributes (first row) and objects (first column).
  Returns also a map which contains attributes, objects and incidence.
  Input csv file is expected to look like:
  ,0,1,2,3,4
  0,0,0,0,1,0
  0,0,1,0,0,1"
  ([^String filepath]
   (let [file-instance (io/file filepath)]  ; Cast to file object
     (if (.exists file-instance)               ; Check if the input file exists
       (with-open [reader (io/reader filepath)]
         (let [data (doall (csv/read-csv reader))]
           (let [all-but-first-row (rest data)
                 first-column (map first all-but-first-row)   ; Extract the first column
                 first-row (rest (first data)) ; Extract first row without leading empty cell
                 incidence-matrix (vec (map #(rest %) all-but-first-row))]; collection of attributes, whose elements are mapped to function values
             {:objects (map #(str "doc_" %) first-column)                         ; documents
              :attributes (map #(str "topic_" %) first-row)                        ; topics
              :incidence (vec (map #(Integer/parseInt %) (apply concat incidence-matrix)) )})))  ; Return as a map

       (do
         (println "File not found!")
         nil)))))  ; Return nil if the file is not found





;;; Usage example
;(println (extract-obj-attr-inc "results/metadata.csv"))
;(println (extract-obj-attr-inc-binary "sample_results/test1510/metadata.csv"))
;(println (type (extract-obj-attr-inc-binary "sample_results/test1510/metadata.csv" 2)))
;(display-bin-ctx (extract-obj-attr-inc-binary "sample_results/test1510/metadata.csv"))
;(println (extract-obj-attr-inc-binary "sample_results/test1911/metadata.csv" 4))
;(println (obtain-bin-ctx (extract-obj-attr-inc-binary "sample_results/test1911/metadata.csv" 4)))
;(println (compute-concept-lattice (obtain-bin-ctx (extract-obj-attr-inc-binary "sample_results/test1911/metadata.csv" 4))))
;(println "Iceberg")
;(println (compute-titanic-iceberg-lattice (obtain-bin-ctx (extract-obj-attr-inc-binary "sample_results/test1911/metadata.csv" 4))))
;
;(let [path2uni-data "/Users/klara/Developer/Uni/WiSe2425/text_topic/results/incidences/thres_row_norm_doc_topic_incidence.csv"
;      map-from-zero-one-csv (zero-one-csv2-map path2uni-data)
;      first-n-docs 10                                       ; (count (:objects map-from-zero-one-csv)) ; 10 produces at most 18 elements in lattice
;      objects (take first-n-docs (:objects map-from-zero-one-csv))
;      attributes (:attributes map-from-zero-one-csv)
;      incidence (take (* first-n-docs 47) (:incidence map-from-zero-one-csv))
;      ctx (contexts/make-context-from-matrix objects attributes incidence)
;      ]
;  (println "Uni Data" (count objects) (count attributes) (first attributes) (count incidence))
;  (println (compute-concept-lattice ctx))
;  (println "All concepts in Lattice:" (contexts/concepts ctx))
;  (println (compute-titanic-iceberg-lattice ctx))
;  )
;
; ; test update-incidence: Add 1 to parent if child is included in parent and child has 1 (binary context)
;(let [data {:objects [".DS_Store" ".Rhistory" ".gitignore"]
;            :attributes ["Downloads" "Downloads/dir1" "Downloads/dir1/dir2"]
;            :incidence [1 0 0 0 1 0 0 0 1]}]
;  (println data)
;  (println (update-incidence data))
;  (display-bin-ctx data)
;  (display-bin-ctx (update-incidence data))
;  )