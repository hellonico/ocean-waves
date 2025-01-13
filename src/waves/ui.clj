(ns waves.ui
  (:gen-class)
  (:require [cljfx.api :as fx]
            ;[cljfx.css :as css]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [pyjama.state]
            [waves.core]
            [waves.fx :refer :all]
            )
  (:import (javafx.scene.image Image)
           (javafx.scene.input DragEvent TransferMode)
           (javafx.stage FileChooser FileChooser$ExtensionFilter)))

(def app-state
  (atom {:file-path       nil
         :url             "http://localhost:11434"
         :debug           true
         :output          nil
         :model           "llama3.2"
         :models          []
         :local-models    []
         ;:system-prompt "You are a machine translator from Japanese to English. You are given one string each time. When the string is in English you return the string as is. If the string is in Japanese, you answer with the best translation. Your answer will only contain the translation and the translation only, nothing else, no question, no explanation. If you do not know, answer with the same string as input"
         :prompt-template "You are a machine translator from Japanese to English. You are given one string each time. When the string is in English you return the string as is. If the string is in Japanese, you answer with the best translation. Your answer will only contain the translation and the translation only, nothing else, no question, no explanation. If you do not know, answer with the same string as input. Translate: %s"
         :status          :idle}))

(defn file-chooser []
  (let [chooser (FileChooser.)]
    (.getExtensionFilters chooser)
    (.add (.getExtensionFilters chooser)
          (FileChooser$ExtensionFilter. "PPT and PPTX Files" ["*.ppt" "*.pptx"]))
    (.showOpenDialog chooser nil)))

(defn long-running-task []
  (waves.core/update-ppt-text app-state))
;
;(def spinner-image
;  (Image. (io/input-stream (io/resource "spinner.gif"))))
;
;(def check-image
;  (Image. (io/input-stream (io/resource "check.png"))))
;
;(def failed-image
;  (Image. (io/input-stream (io/resource "failed.png"))))

; TODO: generic
(defn handle-drag-dropped [event]
  (let [db (.getDragboard event)
        files (.getFiles db)
        file (first files)]
    (when file
      (let [file-path (.getAbsolutePath file)]
        (swap! app-state assoc :file-path file-path)))))

; TODO generic
(defn on-drag-over [^DragEvent event]
    (let [db (.getDragboard event)]
      (when (.hasFiles db)
        (doto event
          (.acceptTransferModes
            (into-array TransferMode [TransferMode/COPY]))))))

;(def style
;  (css/register ::style
;                {":info" {:-fx-background-color "#bbf"
;                          ":hover"              {:-fx-background-color "#ddf"}}
;                 }))
(defn root-view [state]
  {:fx/type          :stage
   :title            "Waves [PPTX Translator]"
   ;:event-handler event-handler
   :showing          true
   :width            500
   :on-close-request (fn [_] (System/exit 0))               ; Exit when the window is closed
   :icons            [(Image. (io/input-stream (io/resource "delicious.png")))] ; Set the app icon
   :height           600
   :scene            {:fx/type :scene
                      :root    {:fx/type  :v-box
                                :spacing  10
                                :stylesheets #{"extra.css"
                                               ;(::css/url style)
                                               (.toExternalForm (io/resource "terminal.css"))}
                                :padding  20
                                :children [
                                           ;{:fx/type :label
                                           ; :text    "PPTX Translator"
                                            ;:classes
                                            ;:pseudo-classes #{:title}
                                            ;}
                                           {:fx/type :label
                                            :text
                                            (if-let [file-path (:file-path @app-state)]
                                              (.getName (io/as-file file-path))
                                              "No file selected")
                                            }
                                           {:fx/type   :button
                                            :text      "Select or Drag File"
                                            ;:h-box/hgrow      :always
                                            :max-width Double/MAX_VALUE
                                            :on-drag-over    on-drag-over
                                            :on-drag-dropped handle-drag-dropped
                                            :on-action (fn [_]
                                                         (let [file (.getAbsolutePath (file-chooser))]
                                                           (when file
                                                             (swap! app-state assoc :file-path file)
                                                             (swap!
                                                               app-state assoc :output
                                                               (waves.utils/compute-output-file-path (:file-path @app-state)))
                                                             )))}

                                           {:fx/type :label :text "URL"}
                                           {:fx/type         :text-field
                                            :text            (:url state)
                                            :on-text-changed #(do
                                                                (if (valid-url? %)
                                                                  (do
                                                                    (swap! app-state assoc :url %)
                                                                    (pyjama.state/local-models app-state))))}
                                           {:fx/type :label
                                            :text "Model"}
                                           {:fx/type   :h-box
                                            :alignment :center-left
                                            :children  [
                                                        {:fx/type          :combo-box
                                                         :items            (:local-models state)
                                                         :value            (:model state)
                                                         :on-value-changed #(swap! app-state assoc :model %)
                                                         }
                                                        (if (not (:loading @app-state))
                                                          {:fx/type          :image-view
                                                           :image            (rsc-image "reload.png")
                                                           :fit-width        24
                                                           :fit-height       24
                                                           :on-mouse-clicked #(async/thread
                                                                                (swap! app-state assoc :loading true)
                                                                                ; keep for now because of %
                                                                                (println %)
                                                                                (try
                                                                                  (pyjama.state/local-models app-state)
                                                                                  (catch Exception _))
                                                                                (swap! app-state assoc :loading false)
                                                                                )
                                                           }
                                                          {:fx/type    :image-view
                                                           :image      (rsc-image "reload.gif")
                                                           :fit-width  24
                                                           :fit-height 24
                                                           })
                                                        ]
                                            }

                                           {:fx/type :label :text "Prompt Template"}
                                           {:fx/type         :text-area
                                            :wrap-text       true
                                            :pref-height     200
                                            :text            (:prompt-template state)
                                            :on-text-changed #(swap! app-state assoc :prompt-template %)}
                                           {:fx/type  :h-box
                                            :spacing  10
                                            :children [{:fx/type   :button
                                                        :text      "Translate"
                                                        :disable   (= :running (:status state)) ; Disable during task
                                                        :on-action (fn [_]
                                                                     (swap! app-state assoc :status :running)
                                                                     (future
                                                                       (try
                                                                         (long-running-task)
                                                                         (swap! app-state assoc :status :completed)
                                                                         (catch Exception e
                                                                           (.printStackTrace e)
                                                                           (swap! app-state assoc :status :failed)))))}
                                                       (case (:status state)
                                                         :idle {:fx/type :region :pref-width 24 :pref-height 24} ; Empty space
                                                         :running {:fx/type    :image-view
                                                                   :image      (rsc-image "spinner.gif")
                                                                   :on-mouse-clicked (fn[_]
                                                                                       (swap! app-state assoc :status :stopping))
                                                                   :fit-width  24
                                                                   :fit-height 24}
                                                         :stopping
                                                         {:fx/type    :label
                                                          :text "Stopping"}
                                                         :completed
                                                         {:fx/type  :h-box
                                                          :spacing  10
                                                          :children [
                                                                     {:fx/type    :image-view
                                                                      :image      (rsc-image "check.png")
                                                                      :fit-width  24
                                                                      :fit-height 24}
                                                                     {:fx/type   :button
                                                                      :text      "Open Output File"
                                                                      :on-action (fn [_]
                                                                                   (open-file (@app-state :output)))}
                                                                     ]
                                                          } ; Success
                                                         :failed {:fx/type    :image-view
                                                                  :image      (rsc-image "failed.png")
                                                                  :fit-width  24
                                                                  :fit-height 24})
                                                       ]

                                            }

                                           ]}}})

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root-view)
    :opts {:app-state app-state}))

(defn -main [& args]
  (async/thread
    (try
      (pyjama.state/local-models app-state)
      (catch Exception e (do
                           (swap! app-state assoc :local-models [] :url "" :model "")
                           ))))
  (fx/mount-renderer app-state renderer))
