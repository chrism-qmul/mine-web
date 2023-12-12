(ns minecraft-web.app
 (:require 
  [reagent.core :as reagent]
  [reagent.dom :as rdom]
  [minecraft-web.util :as util]
  [minecraft-web.game :as game])
	(:import [socket io-client])
)

(def key-code->block-type {
1 "blue"; 86
2 "yellow"; 87
3 "green";88
4 "orange";89
5 "purple";90
6 "red";91
})

   

(def socket (io-client/io.))

(def g (game/create))

;(.. g -controls target -avatar -position toArray)

;(game/add-block! g [1 1 1] "blue")

(defn ^:dev/before-load stop []
  (js/console.log "stop"))

(try
	(.parse js/JSON "[1]")
	(catch js/Error e
		(prn "no parse")))

(def connected (atom false))
(def game-obj (atom nil))
(def game-state (atom nil))
(def world-state (atom nil))
(def history (reagent/atom []))
(def current-model (reagent/atom nil))
;(def prev-utterances (reagent/atom []))

(defn add-block-to-game! [g party position block-type]
	(swap! world-state assoc position block-type)
	;(swap! history conj {:type :action :src party :data [position block-type "putdown"]})
	(game/add-block! g position block-type))

(defn remove-block-from-game! [g party position]
	(swap! world-state dissoc position)
	;(swap! history conj {:type :action :src party :data [position "pickup"]})
	(game/remove-block! g position))

;(.. g -opts)
;(.. g -opts -container)

(defn is-utterance? [h]
	(= :utterance (h :type)))

(defn encoded-game-state []
	(let [dialog (last (into [] (comp (filter is-utterance?) (map :data)) @history))
     	      world-state (str (into [] (for [[k v] @world-state] (conj k v))))
	      payload {:world world-state :dialog dialog}]
	(->> payload
	(clj->js)
	(.stringify js/JSON))))

(defn send-update! []
 (let [encoded-state (encoded-game-state)]
  (.log js/console "sending" encoded-state)
  (.emit socket "update-game" encoded-state)))

(defn game []
 (reagent/create-class                 ;; <-- expects a map of functions
  {:display-name  "voxel-game"      ;; for more helpful warnings & errors
  :component-will-unmount
	(fn [this] 
(.destroy g)
;(.remove (util/$ "#stats"))
)
  :component-did-mount               ;; the name of a lifecycle function
  (fn [this]

   (let [
    ;container (.appendChild (util/$ "div#game") (util/createEl "div"))
    container (util/$ "div#game")
	
;	g (game/create ;{:container container}
;)
]
    (game/add-to-dom g container)
    (game/setup! g game-state)

(util/on-keypress js/window (fn [ev] 
	(let [number-pressed (->> ev (.-key) (js/parseInt))]
		(if (zero? number-pressed)
			 (when-let [{:keys [position]} (game/raycast g)]
				(remove-block-from-game! g "Architect" position)) 
	       		(when-let [block-type (get key-code->block-type number-pressed)] 
				(when-let [position (game/raycast-adjacent g)]
					(add-block-to-game! g "Architect" position block-type)))))))
    ;(prn (.playerPosition game))
    (reset! game-obj g)
   )
  ) ;; your implementation
  :reagent-render        ;; Note:  is not :render
  (fn []           ;; remember to repeat parameters
   [:div#game {:style {:height "100vh"}}])}))

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
;	[:p (str utterances)]
	[:ul.chat (for [[i {:keys [src data]}] (map-indexed vector utterances)
:let [party (if (= src "Architect") "sent" "rcvd")]]
		^{:key i} [:li.msg {:class [party]} 
;[:b (str "<" src ">: ")] 
(str data)])])
;)

(defn select [properties values]
[:select properties 
 (for [[k v] values]
  ^{:key k} [:option {:value k} v])])

(def models 
{:collaborative "Collaborative" 
:learn_to_ask "LearnToAsk"})

(defn legend []
	[:div.legend
		[:b "Controls: "]
	(doall (for [[k color] key-code->block-type]
		[:span {:style {:color color}} (str k)]))
		"("
		[:span {:style {:color "white"}} "0"]
		" removes)"])

(defn chat []
 (let [current-utterance (reagent/atom "")
       ]
  (fn []
   [:div {:style {:display "flex" :height "100vh" :flex-direction "column"}}
   [:h1 {:style {:flex "0 1 auto"}} "Chat"]
   [:div {:style {:flex "1 1 auto" :overflow "auto"}}
   [chat-history @history]]
   [:div {:style {:flex "0 1 auto"}}
   [input-field current-utterance]
   [:button {
   :disabled (empty? @current-utterance)
   :on-click 
   (fn [] 
    (swap! history conj {:type :utterance :src "Architect" :data @current-utterance}) 
    (reset! current-utterance "")
    (send-update!))} "send"]
   ;[select {:name :model 
   ;         :ref (fn [el] (when (some? el) (reset! current-model (. el -value)))) 
   ;         :on-change (fn [ev] (reset! current-model (.. ev -target -value)))} models]
	]])))


(defn root []
 [:div.container>div.row
	[:div.col-sm-3 {:style {:height "100vh"}}
		[chat]]
	[:div.col-sm-9
		[legend]
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
  ;(:actions m)
 ; (map (fn [[k v]] (vector k v)) m)
 ; (into {} m)
))

(defn socket-receive [data]
	(.log js/console "ssocket receive new game state" data)
	(.log js/console "ssocket receive new game state, decoded" (decode-game-state data))
	(let [{:keys [instruction]} (decode-game-state data)]
;[new-actions (map (fn [data] {:type :action :src "Builder" :data data}) data)]
	(when-not (empty? instruction)
	    (swap! history conj {:type :utterance :src "Builder" :data instruction})) 
		)
	;(swap! history concat new-actions)
	;(let [new-game-state (merge (decode-game-state data) @game-state)]
	;(reset! game-state new-game-state)))
)

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
