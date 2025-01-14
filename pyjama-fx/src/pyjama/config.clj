(ns pyjama.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint])
  (:import (java.io FileReader PushbackReader)
           (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)))

(defn get-config-dir
  "Returns the config directory path for the given app."
  [app-name]
  (let [home (System/getenv "HOME")]
    (io/file home ".config" "pyjama" app-name)))

(defn list-config-files
  "Returns a sorted list of `.edn` files in the config directory for the given app."
  [app-name]
  (let [config-dir (get-config-dir app-name)]
    (if (.exists config-dir)
      (->> (file-seq config-dir)
           (filter #(and (.isFile %)
                         (.endsWith (.getName %) ".edn")))
           (sort-by #(.lastModified %))
           reverse)
      [])))
(defn save-atom
  "Saves the content of an atom (a map) to a file with a date-stamped name in a pretty-printed format."
  [app-name state-atom]
  (let [config-dir (get-config-dir app-name)
        timestamp (-> (LocalDateTime/now)
                      (.format (DateTimeFormatter/ofPattern "yyyyMMdd_HHmmss")))
        file-name (str timestamp ".edn")
        file-path (io/file config-dir file-name)]
    (io/make-parents file-path)
    (with-open [writer (io/writer file-path)]
      (pprint/pprint @state-atom writer))))

(defn load-latest
  "Loads the latest `.edn` file from the config directory, or returns an empty map if no file is found."
  [app-name]
  (if-let [latest-file (first (list-config-files app-name))]
    (with-open [rdr (PushbackReader. (FileReader. latest-file))]
      (edn/read rdr))
    {}))

(defn save-on-shutdown [app-name app-state]
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(pyjama.config/save-atom app-name app-state))))

(defn load-on-start [app-name app-state]
  (let [latest (pyjama.config/load-latest app-name)]
    (if (seq latest)
      (reset! app-state latest))))

(defn shutdown-and-startup [app-name app-state]
  (save-on-shutdown app-name app-state)
  (load-on-start app-name app-state))