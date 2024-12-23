(ns core
  (:gen-class)
  (:require [clojure.tools.cli])
  (:require [waves.core]))

(def cli-options
  [["-i" "--input INPUT" "Input PowerPoint file (default: input.pptx)"
    :default "input.pptx"]
   ["-o" "--output OUTPUT" "Output PowerPoint file (default: output.pptx)"
    :default "output.pptx"]
   ["-m" "--model MODEL" "Model to use (default: llama3.2)"
    :default "qwen"]
   ["-u" "--url URL" "Server URL (default: http://localhost:11434)"
    :default "http://localhost:11434"]
   ["-s" "--system-prompt PROMPT" "System prompt (default: translation prompt)"
    :default "You are a machine translator from Japanese to English. You are given one string each time. When the string is in English you return the string as is. If the string is in Japanese, you answer with the best translation. Your answer will only contain the translation and the translation only, nothing else, no question, no explanation. If you do not know, answer with the same string as input"]
   ["-p" "--prompt-template TEMPLATE" "Prompt template (default: %s)"
    :default "%s"]
   ["-d" "--debug" "Enable debug mode (default: false)"
    :default false
    :parse-fn #(Boolean/valueOf ^String %)]])

(defn -main [& args]
  (let [{:keys [options errors summary]} (clojure.tools.cli/parse-opts args cli-options)]
    (clojure.pprint/pprint options)

    (if (seq errors)
      (do
        (println "Error parsing options:")
        (doseq [err errors] (println "  " err))
        (println "\nUsage:\n" summary)
        (System/exit 1))

      (let [{:keys [^String input ^String output model url system-prompt prompt-template debug]} options]
        (waves.core/update-ppt-text
          (String. input)
          (String. output)
          url
          model
          system-prompt
          prompt-template
          debug)))))