(ns waves.core
  (:require [pyjama.core] [waves.utils])
  (:import [org.apache.poi.xslf.usermodel XMLSlideShow XSLFSlide XSLFTextShape]
           [java.io FileInputStream FileOutputStream]))

(defn update-ppt-text [input-path output-path options]
  (with-open [input-stream (FileInputStream. ^String input-path)
              output-stream (FileOutputStream. ^String output-path)]
    (let [ppt (XMLSlideShow. input-stream)]
      (doseq [^XSLFSlide slide (.getSlides ppt)]
        ;(println (count (.getShapes slide)))
        (doseq [shape (.getShapes slide)]
          (when (instance? XSLFTextShape shape)
            (when-let [text (.getText ^XSLFTextShape shape)]
              (if (and (not (nil? text)) (not (clojure.string/blank? text)))
                (let [translation (waves.utils/translate options text)]
                  (if (not (nil? translation))
                    (try
                      (.setText ^XSLFTextShape shape translation)
                      (catch Exception e
                        (clojure.pprint/pprint options)
                        (print *err* "ERROR:\n" (.getMessage e) "\n" translation)
                        (throw (Exception. (str "ERROR:\n" (.getMessage e) "\n" translation)))))
                      )))))))
        (.write ppt output-stream))))