(ns waves.fx
  (:require [clojure.java.io :as io])
  (:import (java.awt Desktop)
           (java.net URL)
           (javafx.scene.image Image)))

(defn open-file [file-path]
  (let [desktop (Desktop/getDesktop)]
    (.open desktop (clojure.java.io/as-file file-path))))

(defn rsc-image[file]
  (Image. (io/input-stream (io/resource file))))

(defn valid-url? [url]
  (try
    (URL. url)
    true
    (catch Exception _ false)))
