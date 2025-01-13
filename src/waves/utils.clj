(ns waves.utils
  (:require [clojure.java.io :as io]))

(defn translate[config text]
  (if (:debug config)
    (println ":> " text "\n"))

(let [
      _options (if (contains? config :prompt)
                 config
                 (conj {:prompt (format (:prompt-template config) text)} config))
      ; TODO: pass through
      translation (clojure.string/trim
                    (pyjama.core/ollama
                      (:url config)
                      :generate
                      _options
                      :response
                      ))
      ]

  (if (:debug config)
    (println "< " translation "\n"))

  translation
  ))

(defn compute-output-file-path [input-file-path]
  (let [file (io/file input-file-path)
        parent-dir (.getParent file)
        original-name (.getName file)
        translated-name (str "translated_" original-name)]
    (str parent-dir "/" translated-name)))
