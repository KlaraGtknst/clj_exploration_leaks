(ns clj-exploration-leaks.core
  (:require [conexp.fca.contexts :as contexts]
            [conexp.fca.many-valued-contexts :as mv_contexts]
            ))

;; this file is a toy example of how to use the conexp-clj library
;; use '''lein repl''' to run repl in terminal
;; use this syntax for require:
;;  (require '[conexp.fca.contexts :as contexts])

(def objects ["apple" "banana" "cherry"])
(def attributes ["color" "taste" "texture"])
(def incidence [["apple" "color"]
                ["banana" "taste"]
                ["cherry" "texture"]
                ["banana" "color"]])

(def ctx-1 (contexts/make-context objects attributes incidence)) ; context via list of present indices

(println "Binary-valued context: " ctx-1)

(def ctx (contexts/make-context-from-matrix ['a 'b 'c]      ; context via adjacency matrix
                                   [1 2 3]
                                   [0 1 0
                                    1 1 0
                                    1 0 1]))
(println "Binary context: " ctx)
(println "objects of context: " (contexts/objects ctx-1))
(println "attributes of context: " (contexts/attributes ctx-1))
(println "incidence of context: " (contexts/incidence ctx-1))
(println "number of formal concepts:" (count (contexts/concepts ctx)))

;; multi-value
(def mv_objects ["apple" "banana" "cherry"])
(def mv_attributes ["color" "taste" "texture"])
(def mv_incidence ["green" "good" "solid"
                   "yellow" "ok" "cozy"
                   "red" "good" "solid"])
(println "Incidence of multi-valued context: " mv_incidence)
(println "Multi-valued context: " (mv_contexts/make-mv-context-from-matrix mv_objects mv_attributes mv_incidence))
