(ns clj-exploration-leaks.core
  (:require [conexp.fca.contexts :as contexts]
    [conexp.fca.many-valued-contexts :as mv_contexts]
    [clj-exploration-leaks.io.csv2ctx :as csv2ctx]
    [conexp.api.handler :as handler] ; FIXME: more recent version of the Java Runtime (class file version 66.0) -> openjdk-23
    [conexp.gui.draw :as draw]       ; FIXME: more recent version of the Java Runtime (class file version 66.0) -> openjdk-23
    ;[conexp.fca.lattices :as concept]
            ))

;; use '''lein repl''' to run repl in terminal
;; use this syntax for require:
;;  (require '[conexp.fca.contexts :as contexts])

(defn foo-
  "I don't do a lot."
  [x]
  (println x "Hello, World!"))

;; (def ctx-1 (contexts/make-context [1 2 3] [4 5 6] '[0 0 0 0 0 0 0 0 1])) ;; nth not supported on this type: Long
;; (def ctx-1 (contexts/make-context [1 2 3] [4 5 6]))  ;; Wrong number of args (2) passed to: conexp.fca.contexts/eval1919/fn--1920

(def objects ["apple" "banana" "cherry"])
(def attributes ["color" "taste" "texture"])
(def incidence [["apple" "color"]
                ["banana" "taste"]
                ["cherry" "texture"]
                ["banana" "color"]])

(def ctx-1 (contexts/make-context objects attributes incidence)) ; context via list of present indices

#_(println ctx-1)

(def ctx (contexts/make-context-from-matrix ['a 'b 'c]      ; context via adjacency matrix
                                   [1 2 3]
                                   [0 1 0
                                    1 1 0
                                    1 0 1]))
#_(println ctx)
#_(println (contexts/objects ctx-1))
#_(println (contexts/attributes ctx-1))
#_(println (contexts/incidence ctx-1))
#_(println "number of formal concepts:" (count (contexts/concepts ctx)))

(csv2ctx/display-bin-ctx (csv2ctx/extract-obj-attr-inc-binary "sample_results/test1510/metadata.csv"))
(println "new test")
(csv2ctx/display-bin-ctx (csv2ctx/extract-obj-attr-inc-binary "sample_results/test1510/metadata.csv" 2))

;; multi-value
(def mv_objects ["apple" "banana" "cherry"])
(def mv_attributes ["color" "taste" "texture"])
(def mv_incidence ["green" "good" "solid"
                   "yellow" "ok" "cozy"
                   "red" "good" "solid"])
#_(println "Incidence:" mv_incidence)
#_(println (mv_contexts/make-mv-context-from-matrix mv_objects mv_attributes mv_incidence))

; displays metadata as multi-valued context
#_(let [file_path "sample_results/test1510/metadata.csv"
      ctx_map (csv2ctx/extract-obj-attr-inc file_path)]
  (println (mv_contexts/make-mv-context-from-matrix (:objects ctx_map) (:attributes ctx_map) (:incidence ctx_map))))

; (println (contexts/read-fca jsonfile.json)); https://github.com/tomhanika/conexp-clj/blob/master/doc/IO.org


;; https://github.com/tomhanika/conexp-clj/blob/master/doc/Common-FCA-File-Formats-for-Formal-Contexts.orghttps://github.com/tomhanika/conexp-clj/blob/master/doc/Common-FCA-File-Formats-for-Formal-Contexts.org
;; TODO: why not found if present in tutorial?
#_(let [context_path "sample_results/context.csv"]
  (handler/write-context :named-binary-csv ctx context_path)
(handler/read-data context_path                             ; :named-binary-csv
                   )
)

;; visualization
;; FIXME: Execution error: no/geosoft/cc/graphics/GWindow has been compiled by a more recent version of the Java Runtime (class file version 66.0), this version of the Java Runtime only recognizes class file versions up to 65.0
;; requires openjdk-23-jdk; I currently can't install that since fski-nexus does not provide that
; (draw/draw-lattice (concept/concept-lattice (contexts/adiag-context 2)))



