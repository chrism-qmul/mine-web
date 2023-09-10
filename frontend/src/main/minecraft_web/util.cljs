(ns minecraft-web.util)

(def $ #(.querySelector js/document %))

(def createEl #(.createElement js/document %))

(defn on-keypress [listener]
	(.addEventListener js/window "keydown" (fn [ev] (listener ev)) false))
