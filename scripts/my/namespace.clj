(ns my.namespace
  (:require [clojure.string :as str])
  (:import (org.reflections Reflections)
           (org.reflections.scanners TypeElementsScanner)))

(defn find-classes [package-names]
      (let [scanner (TypeElementsScanner.)]  ;; Use the newer TypeElementsScanner
           (->> package-names
                (mapcat (fn [package-name]
                            (let [reflections (Reflections. package-name (into-array [scanner]))]
                                 (filter #(str/starts-with? % package-name) (.getAllTypes reflections)))))
                distinct)))

(defn format-class-output [class-name]
      (str "{\"name\": \"" class-name "\", "
           "\"allDeclaredConstructors\": true, "
           "\"allDeclaredFields\": true, "
           "\"allDeclaredMethods\": true}"))

(defn find-and-print-classes
      [{:keys [packages]}]
      (if packages
        (let [package-names (str/split packages #",")
              classes (find-classes package-names)]
             (doseq [class-name classes]
                    (println (format-class-output class-name))))
        (println "Error: Please provide a comma-separated list of package names via :packages argument.")))
