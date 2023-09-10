(ns minecraft-web.app
 (:require 
  [reagent.core :as reagent]
  [reagent.dom :as rdom]
  [minecraft-web.util :as util]
  [minecraft-web.game :as game])
	(:import [socket io-client])
)

;(def )

(def socket (io-client/io.))

(defn ^:dev/before-load stop []
  (js/console.log "stop"))

(try
	(.parse js/JSON "[1]")
	(catch js/Error e
		(prn "no parse")))

(def connected (atom false))
(def game-obj (atom nil))
(def game-state (atom nil))
(def prev-utterances (reagent/atom []))


(defn encode-game-state []
	(let [world-state (map (fn [[k v]] (vector k (- v 85))) @game-state)
	      payload {:world-state world-state :dialog @prev-utterances}]
	(->> payload
	(clj->js)
	(.stringify js/JSON))))

(defn send-update! []
 (let [encoded-state (encode-game-state)]
  (.log js/console "sending" encoded-state)
  (.emit socket "update-game" encoded-state)))

(defn game []
 (reagent/create-class                 ;; <-- expects a map of functions
  {:display-name  "my-component"      ;; for more helpful warnings & errors
  :component-did-mount               ;; the name of a lifecycle function
  (fn [this]

   (let [
    container (.appendChild (util/$ "div#game") (util/createEl "div"))
	g (game/create {:container container})]
    (game/add-to-dom g container)
    (game/setup! g game-state)
    ;(prn (.playerPosition game))
    (reset! game-obj g)
   )
  ) ;; your implementation
  :reagent-render        ;; Note:  is not :render
  (fn []           ;; remember to repeat parameters
   [:div#game])}))

(defn add-random-block []
  (.log js/console (.-createBlock @game-obj))
 (.createBlock @game-obj [1 63 1] 86))

;(.setBlock @game-obj [0 63 0] 86;)

(defn input-field [state]
	[:input {:type        "text"
           :value       @state
           :on-change   (fn [event]
                          (reset! state (-> event .-target .-value)))}])

(defn chat-history [utterances]
		[:ul (for [[i utterance] (map-indexed vector utterances)]
		^{:key i} [:li utterance])])
(defn chat []
	(let [current-utterance (reagent/atom "")]
	[:div 
		[chat-history @prev-utterances]
		[:div
			[input-field current-utterance]
			[:button {:on-click 
(fn [] 
(swap! prev-utterances conj (str "<Architect>: " @current-utterance)) 
(reset! current-utterance "")
(send-update!))} "send"]]]))

(defn root []
 [:div.container>div.row
	[:div.col-sm-3
		[:h1 "Chat"]
		[chat]]
	[:div.col-sm-9
		[game]]
	])

(defn mount-root []
  (rdom/render [root] (.getElementById js/document "root")))

(defn ^:dev/after-load start []
  (js/console.log "start")
  (mount-root))

(defn decode-game-state [state]
 (as-> state m
  (js->clj m :keywordize-keys true)
  (:world-state m)
  (map (fn [[k v]] (vector k (+ v 85))) m)
  (into {} m)))

(defn socket-receive [data]
	(.log js/console "ssocket receive new game state" data)
	(let [new-game-state (merge (decode-game-state data) @game-state)]
	(.log js/console "socket receive new game state" data new-game-state)
	(reset! game-state new-game-state)))

(defn init []
  (js/console.log "init")
  (let []
	(.on socket "update-game" socket-receive)
	(.on socket "disconnect" #(reset! connected false))
	(.on socket "connect"
		(fn [] 
			(reset! connected true)
			(.log js/console "connected!")
			))
	(mount-root)))
	;

(comment (prn "hello world"))

;(prn @game-state)
(comment (swap! game-state assoc [2 63 -4] 86))
(comment (swap! game-state dissoc [2 63 -3] 86))
;(swap! game-state assoc [2 63 -4] 86)
