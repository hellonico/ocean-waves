(ns waves.core2
  (:require [pyjama.core] [waves.utils])
  (:import [org.docx4j.openpackaging.packages PresentationMLPackage]
           [org.docx4j.openpackaging.parts.PresentationML SlidePart]
           [org.docx4j.dml CTRegularTextRun CTTextParagraph]
           (org.pptx4j.pml Shape Sld)))

(defn get-shape-full-text [shape]
  (when-let [text-body (.getTxBody shape)]
    (->> (.getP text-body)                                      ;; Get all paragraphs
         (mapcat #(.getEG_TextRun %))                           ;; Extract all text runs from each paragraph
         (filter #(instance? CTRegularTextRun %)) ;; Filter for regular text runs
         (map #(.getT %))                                       ;; Get the text from each text run
         (filter some?)                                         ;; Remove nil values
         (reduce str))))                                        ;; Combine into a single string

(defn prefix-text [pptx-path output-path config]
  (try
    (let [presentation (PresentationMLPackage/load (clojure.java.io/as-file (str pptx-path)))]
      (doseq [slide (.getSlideParts (.getMainPresentationPart presentation))]
        (doseq [shape (.getSpOrGrpSpOrGraphicFrame (.getSpTree (.getCSld ^Sld (.getJaxbElement ^SlidePart slide))))]
          (if (instance? Shape shape)
            (when-let [text-body (.getTxBody ^Shape shape)]
              (doseq [paragraph (.getP text-body)]
                (doseq [text-run (.getEGTextRun ^CTTextParagraph paragraph)]
                  (when (instance? CTRegularTextRun text-run)
                    (let [current-text (.getT ^CTRegularTextRun text-run)]
                      (when current-text
                        (let [translation (waves.utils/translate config current-text)]
                          (.setT ^CTRegularTextRun text-run (clojure.string/trim translation))))))))))))
        (.save presentation (clojure.java.io/as-file output-path)))
      (catch Exception e
        (.printStackTrace e)
        (println "Error updating presentation:" (.getMessage e)))))