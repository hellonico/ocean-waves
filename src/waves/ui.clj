(ns waves.ui
  (:gen-class)
  (:import [javafx.scene.image Image])
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io]
            [waves.core]
            [waves.core2]))

(def app-state
  (atom {:file-path nil
         :url "http://localhost:11434"
         :debug true
         :output nil
         :model "llama3.2"
         :models []
         ;:system-prompt "You are a machine translator from Japanese to English. You are given one string each time. When the string is in English you return the string as is. If the string is in Japanese, you answer with the best translation. Your answer will only contain the translation and the translation only, nothing else, no question, no explanation. If you do not know, answer with the same string as input"
         :prompt-template "You are a machine translator from Japanese to English. You are given one string each time. When the string is in English you return the string as is. If the string is in Japanese, you answer with the best translation. Your answer will only contain the translation and the translation only, nothing else, no question, no explanation. If you do not know, answer with the same string as input. Translate: %s"
         :status :idle})) ; :idle, :running, or :completed

;; Fetch models from the URL
(defn fetch-models [url on-complete]
  (pyjama.core/ollama url :tags {}
                      (fn [res] (on-complete (map :name (:models res))))))

;; When the URL changes, refresh the model list
(defn update-model-list [url]
  (fetch-models url
                (fn [models]
                  (swap! app-state assoc :models models))))

(defn file-chooser []
  (let [chooser (javafx.stage.FileChooser.)]
    (.getExtensionFilters chooser)
    (.add (.getExtensionFilters chooser)
          (javafx.stage.FileChooser$ExtensionFilter. "PPT and PPTX Files" ["*.ppt" "*.pptx"]))
    (.showOpenDialog chooser nil)))

(defn long-running-task []
  ;(waves.core2/prefix-text input output @app-state)
  (waves.core/update-ppt-text (@app-state :file-path) (@app-state :output) @app-state))

(def spinner-image
  (Image. (io/input-stream (io/resource "spinner.gif"))))

(def check-image
  (Image. (io/input-stream (io/resource "check.png"))))

(def failed-image
  (Image. (io/input-stream (io/resource "failed.png"))))

;; Simulate long-running task
(defn run-task [file-path on-complete]
  (let [f (future
    (try
      ;(Thread/sleep 5000) ; Simulating long-running task
      (long-running-task)
      (let [output-file-path (waves.utils/compute-output-file-path file-path)]
        (on-complete :success output-file-path))
      (catch Exception e
        (on-complete :failure nil))))]
    ))

;; Stop task
(defonce task-stop-flag (atom false))

(defn root-view [state]
  {:fx/type :stage
   :title "PPTX Translator"
   :showing true
   :width  500
   :on-close-request (fn [_] (System/exit 0)) ; Exit when the window is closed
   :icons [(Image. (io/input-stream (io/resource "delicious.png")))] ; Set the app icon
   :height 600
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :spacing 10
                  :padding 20
                  :children [
                             ;{:fx/type :menu-bar
                             ; :menus [{:fx/type :menu
                             ;          :text "File  "
                             ;          :items [{:fx/type :menu-item
                             ;                   :text "Quit"
                             ;                   :on-action (fn [_] (System/exit 0))}]}]}
                             {:fx/type :label
                              :text "PPTX Translator"}
                             {:fx/type :label
                              :text (or (:file-path state) "No file selected")}
                             {:fx/type :button
                              :text "Select File"
                              :on-action (fn [_]
                                           (let [file (.getAbsolutePath (file-chooser))]
                                             (when file
                                               (swap! app-state assoc :file-path file)
                                               (swap!
                                                 app-state assoc :output
                                                 (waves.utils/compute-output-file-path (:file-path @app-state)))
                                               )))}
                             {:fx/type :label :text "URL"}
                             {:fx/type :text-field
                              :text (:url state)
                              :on-text-changed #(do
                                                  (swap! app-state assoc :url %)
                                                  (update-model-list %))}
                             ;{:fx/type :label :text "Model"}
                             ;{:fx/type :text-field
                             ; :text (:model state)
                             ; :on-text-changed #(swap! app-state assoc :model %)}
                             {:fx/type :label :text "Model"}
                             {:fx/type :combo-box
                              :items (:models state)
                              :value (:model state)
                              :on-action #(swap! app-state assoc :model (.. % -newValue))}

                             ;{:fx/type :label :text "System Prompt"}
                             ;{:fx/type :text-area
                             ; :wrap-text true
                             ; :pref-height 100
                             ; :text (:system-prompt state)
                             ; :on-text-changed #(swap! app-state assoc :system-prompt %)}
                             {:fx/type :label :text "Prompt Template"}
                             {:fx/type :text-area
                              :wrap-text true
                              :pref-height 200
                              :text (:prompt-template state)
                              :on-text-changed #(swap! app-state assoc :prompt-template %)}
                             {:fx/type :h-box
                              :spacing 10
                              :children  [{:fx/type :button
                                           :text "Translate"
                                           :disable (= :running (:status state)) ; Disable during task
                                           :on-action (fn [_]
                                                        (swap! app-state assoc :status :running)
                                                        (future
                                                          (try
                                                            (long-running-task) ; Simulate success
                                                            (swap! app-state assoc :status :completed)
                                                            (catch Exception e
                                                              (.printStackTrace e)
                                                              (swap! app-state assoc :status :failed)))))}
                                          (case (:status state)
                                            :idle {:fx/type :region :pref-width 24 :pref-height 24} ; Empty space
                                            :running {:fx/type :image-view
                                                      :image spinner-image
                                                      :fit-width 24
                                                      :fit-height 24} ; Spinner
                                            :completed
                                            {:fx/type :h-box
                                             :spacing 10
                                             :children  [
                                            {:fx/type :image-view
                                                        :image check-image
                                                        :fit-width 24
                                                        :fit-height 24}
                                                         {:fx/type :button
                                                          :text "Open Output File"
                                                          :on-action (fn [_]
                                                                       (waves.utils/open-file (@app-state :output)))}
                                                         ]
                                             } ; Success
                                            :failed {:fx/type :image-view
                                                     :image failed-image
                                                     :fit-width 24
                                                     :fit-height 24})
                                          ]

                              }

                                          ]}}})

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root-view)
    :opts {:app-state app-state}))

(defn -main [& args]
  (update-model-list (:url @app-state))
  (fx/mount-renderer app-state renderer))
