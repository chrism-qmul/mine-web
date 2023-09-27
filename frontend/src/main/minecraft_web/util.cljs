(ns minecraft-web.util)

(def $ #(.querySelector js/document %))

(def createEl #(.createElement js/document %))

(defn on-keypress [el listener]
	(.addEventListener el "keydown" (fn [ev] (listener ev)) false))
