(ns waves.utils
  (:import [java.awt Desktop])
  (:require [clojure.java.io :as io]))

(defn translate[config text]
  (if (:debug config)
    (println ":> " text "\n"))

(let [
      ;_ (println config)
      _options (if (contains? config :prompt)
                 config
                 (conj {:prompt (format (:prompt-template config) text)} config))
      ;_ (if (:debug config)
      ;    (clojure.pprint/pprint _options))
          ;(println ":> " (:prompt _options) "\n"))

      ; TODO: pass through
      translation (clojure.string/trim
                    (pyjama.core/ollama
                      (:url config)
                      :generate
                      _options
                      :response
                      ))
      ;translation text
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

(defn open-file [file-path]
  (let [desktop (Desktop/getDesktop)]
    (.open desktop (clojure.java.io/as-file file-path))))