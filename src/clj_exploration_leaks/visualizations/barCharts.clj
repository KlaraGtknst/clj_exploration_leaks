(ns clj-exploration-leaks.visualizations.barCharts
  #_(:require [plotly-clj.core :as plt]
            [clojure.java.io :as io])
  (:use [plotly-clj.core :as plt]))

;; This file provides methods to visualize data via bar charts
;; The code is inspired by https://clojurepatterns.com/8/13/10/ (08.10.2024)
;; FIXME: does not work

(offline-init)

#_(def data [{:x ["Category A" "Category B" "Category C"]
            :y [4 9 2]
            :type "bar"}])

;; Create the plot
#_(-> (plotly {:x ["giraffes" "orangutans" "monkeys"]})
    (add-bar
      :x ["giraffes" "orangutans" "monkeys"]
      :y [20 14 23]
      :name "SF Zoo")
    (plot "Basic-Bar-Chart" :fileopt "overwrite")
    (save-html "plotly.html" :open))



#_(def plot (plt/plot {:data data} "sample_results/"))

;; Save it to an HTML file
#_(spit "bar_chart.html" (plt/html plot))


;; https://github.com/findmyway/plotly-clj?tab=readme-ov-file
(-> (plotly [2 1 3])
    plt/add-scatter
    iplot)
