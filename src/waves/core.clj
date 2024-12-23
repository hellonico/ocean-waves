(ns waves.core
  (:require [pyjama.core])
  (:import [org.apache.poi.xslf.usermodel XMLSlideShow XSLFSlide XSLFTextShape]
           [java.io FileInputStream FileOutputStream]))

(defn strlen-exceeds? [s n]
  (< n (count s)))

(defn update-ppt-text [input-path output-path url model system-prompt prompt-template debug]
  (let
    [
     options
     {:model model
      :stream false
      :system system-prompt
      }
     ]

    (with-open [input-stream (FileInputStream. ^String input-path)
                output-stream (FileOutputStream. ^String output-path)]
      (let [ppt (XMLSlideShow. input-stream)]
        (doseq [^XSLFSlide slide (.getSlides ppt)]
          (doseq [shape (.getShapes slide)]
            (when (instance? XSLFTextShape shape)
              (when-let [text (.getText ^XSLFTextShape shape)]
                (if (and (not (nil? text)) (not (clojure.string/blank? text)))
                  (let [
                        _options (conj {:prompt (format prompt-template text)}
                                       options)
                        ;_ (clojure.pprint/pprint _options)
                        translation (clojure.string/trim
                                      (pyjama.core/ollama
                                      url
                                      :generate
                                      _options
                                      :response
                                      ))
                        ]

                    (if debug
                      (println "> " text "\n")
                      (println "< " translation "\n"))
                    ;
                    ;(if (strlen-exceeds? translation 100)
                    ;  (println "LONG:" translation))

                    (if (not (nil? translation))
                      (try
                        (.setText ^XSLFTextShape shape translation)
                        (catch Exception e
                          (clojure.pprint/pprint _options)
                          (print *err* "ERROR:\n" (.getMessage e) "\n" translation)))))))))

          (.write ppt output-stream))))))
