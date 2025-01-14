(ns waves.core
  (:require [pyjama.core]
            [waves.utils])
  (:import (java.io FileInputStream FileOutputStream)
           (org.apache.poi.xslf.usermodel XMLSlideShow XSLFSlide XSLFTable XSLFTableCell XSLFTextShape)))

(defn update-ppt-text [ app-state ]
  ;(prn @app-state)
  (let [
        input-path (:file-path @app-state )
        ;_ (prn input-path)
        output-path (or (:output @app-state ) (waves.utils/compute-output-file-path input-path))
        ;_ (prn output-path)
        options  @app-state
        ]
  (with-open [input-stream (FileInputStream. ^String input-path)
              output-stream (FileOutputStream. ^String output-path)]
    (let [ppt (XMLSlideShow. input-stream)]
      (doseq [^XSLFSlide slide (.getSlides ppt)]
        (doseq [shape (.getShapes slide)]
          ;; Process table shapes
          (when (instance? XSLFTable shape)
            (doseq [row (.getRows ^XSLFTable shape)]
              (doseq [^XSLFTableCell cell (.getCells row)]
                (let [tx-body (.getTextBody cell)] ;; Get the text body from the cell
                  (when (some? tx-body)
                    (doseq [paragraph (.getParagraphs tx-body)] ;; Iterate over paragraphs
                      (let [original-text (.getText paragraph)]
                        (when (and (not (clojure.string/blank? original-text)))
                          (let [translation (waves.utils/translate options original-text)]
                            (when translation
                              (try (.setText paragraph translation) (catch Exception e (println (.getMessage e))))))
                          ; D: use state here, and stop processing by using throw
                          (if (not (= :running (:status @app-state)))
                            (throw (Exception. (str "Processing Interrupted:" @app-state ))))
                          ))))))))
          ;; Process other shapes (e.g., text shapes)
          (when (instance? XSLFTextShape shape)
            (let [text (.getText ^XSLFTextShape shape)]
              (when (and (not (clojure.string/blank? text)))
                (let [translation (waves.utils/translate options text)]
                  (when translation
                    (try  (.setText ^XSLFTextShape shape translation) (catch Exception e (println (.getMessage e)))))))))))
      (.write ppt output-stream)))))
