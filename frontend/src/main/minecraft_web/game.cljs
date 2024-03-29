(ns minecraft-web.game
 (:require 
  [minecraft-web.util :as util]
  [voxel-engine]
  [voxel-player]
  [medley.core :as m]
  [voxel-fly]
  [voxel-walk]
  [voxel-highlight :as highlight]))

(def $ #(.querySelector js/document %))
(def createEl #(.createElement js/document %))

(def idx->color ["blue" "yellow" "green" "orange" "purple" "red"])

(def color->idx (into {} (map-indexed #(vector %2 %1) idx->color)))

(defn decode-color [color]
 (-> color
  (- 86)
  (idx->color)))

(defn encode-color [color]
 (-> color
  (color->idx)
  (+ 86)))

(defn decode-coord [[x y z]]
	(vector x (- y 63) z))

(defn encode-coord [[x y z]]
	(vector x (+ 63 y) z))

(def TEXTURE_PATH "https://cdn.jsdelivr.net/gh/snyxan/myWorld@main/textures/")

(def MATERIAL_MAP [ 
"blocks/stone" 
"blocks/stone_granite" 
"blocks/stone_granite_smooth" 
"blocks/stone_diorite" 
"blocks/stone_diorite_smooth" 
"blocks/stone_andesite" 
"blocks/stone_andesite_smooth" 
["blocks/grass_top" "blocks/dirt" "blocks/grass_side"] 
"blocks/dirt" 
"blocks/coarse_dirt" 
["blocks/dirt_podzol_top" "blocks/dirt_podzol_side"] 
"blocks/cobblestone" 
"blocks/planks_oak" 
"blocks/planks_spruce" 
"blocks/planks_birch" 
"blocks/planks_jungle" 
"blocks/planks_acacia" 
"blocks/planks_oak" 
"blocks/sapling_oak" 
"blocks/sapling_spruce" 
"blocks/sapling_birch" 
"blocks/sapling_jungle" 
"blocks/sapling_acacia" 
"blocks/sapling_oak" 
"blocks/bedrock" 
"blocks/water_still" 
"blocks/water_flow" 
"blocks/sand" 
"blocks/red_sand" 
"blocks/gravel" 
["blocks/log_oak_top" "blocks/log_oak"] 
["blocks/log_spruce_top" "blocks/log_spruce"] 
["blocks/log_birch_top" "blocks/log_birch"] 
["blocks/log_jungle_top" "blocks/log_jungle"] 
"blocks/leaves_oak" 
"blocks/leaves_spruce" 
"blocks/leaves_birch" 
"blocks/leaves_jungle" 
[ "blocks/sandstone_top" "blocks/sandstone_bottom" "blocks/sandstone_normal" ] 
"blocks/sandstone_smooth"  
"blocks/sandstone_smooth" 
"blocks/deadbush" 
["blocks/grass_top" "blocks/grass_side"] 
"blocks/fern" 
"blocks/deadbush" 
"blocks/wool_colored_white" 
"blocks/wool_colored_orange" 
"blocks/wool_colored_magenta" 
"blocks/wool_colored_light_blue" 
"blocks/wool_colored_yellow" 
"blocks/wool_colored_lime" 
"blocks/wool_colored_pink"
"blocks/wool_colored_gray"
"blocks/wool_colored_silver"
"blocks/wool_colored_cyan"
"blocks/wool_colored_purple"
"blocks/wool_colored_blue"
"blocks/wool_colored_brown"
"blocks/wool_colored_green"
"blocks/wool_colored_red"
"blocks/wool_colored_black"
"blocks/flower_dandelion"
"blocks/flower_tulip_red"
"blocks/mushroom_brown"
"blocks/mushroom_red"
"blocks/hardened_clay_stained_lime"
"letter_r"
"n_n"
"o_n"
"r_n"
"t_n"
"h_n"
"s_s"
"o_s"
"u_s"
"t_s"
"h_s"
"e_e"
"a_e"
"s_e"
"t_e"
"w_w"
"e_w"
"s_w"
"t_w"
"wireframed_blue"
"wireframed_yellow"
"wireframed_green"
"wireframed_orange"
"wireframed_purple"
"wireframed_red"
])

(def SPEAKER_SKIN_PATH (str TEXTURE_PATH "agent.png"))

(def map-coords  
{[-2 62 -9] 68
  [-1 62 -9] 69
  [0 62 -9] 70
  [1 62 -9] 71
  [2 62 -9] 72
  
  [2 62 9] 73
  [1 62 9] 74
  [0 62 9] 75
  [-1 62 9] 76
  [-2 62 9] 77
  
  [9 62 -2] 78
  [9 62 -1] 79
  [9 62 0] 80
  [9 62 1] 81
  
  [-9 62 1] 82
  [-9 62 0] 83
  [-9 62 -1] 84
  [-9 62 -2] 85})

(defn inside-floor? [[x _ z]] 
	(and (<= -5 x 5) (<= -5 z 5)))

(defn generate-basic-map [x y z] 
  (let [map-coord (get map-coords [x y z])
	too-high? (>= y 63)
	outside-floor? (not (inside-floor? [x y z]))
	is-even? (even? (+ x z))]
	(cond 
	  too-high? 0
	  (some? map-coord) map-coord
	  outside-floor? 53
	  is-even? 46
	  :else 54)))

;(def world-origin [0 63 0])
(def world-origin [0 63 0])

(def default-opts
 {
 :generate generate-basic-map
 :chunkDistance 2
 :materials MATERIAL_MAP
 :worldOrigin world-origin
 :controls {:discreteFire true}
 :texturePath TEXTURE_PATH})

(defn create
	([opts] (voxel-engine. (clj->js (merge default-opts opts))))
	([] (create {})))

(defn get-target [game]
	(.. game -controls target))

(defn setup-highlight! [game]
	(highlight game #js{:color 0xff0000}))

(defn setup-player! [game]
 (let [player ((voxel-player game) SPEAKER_SKIN_PATH)]
  (.possess player)
  ((.. player -yaw -position -set) 2 14 4)
  (.set player.position 0 63 0)
  ;(.toggle player)
  (.startFlying ((voxel-fly game) (get-target game)))
  (.control game player)))

(defn listen-world-change [game state]
 (comment 
(.on game "setBlock" (fn [pos new-val old-val]
	(let [pos (decode-coord (js->clj pos))]
	(if (zero? new-val)
		;(do 
		;(.log js/console "remove" (type (js->clj pos)) pos)
		(swap! state dissoc pos)
		;(do 
		;(.log js/console "add" (type (js->clj pos)) pos)
		(swap! state assoc (pos (decode-color new-val)))
;	(.log js/console "world change" @state ":" pos new-val old-val)
)))
))
 (prn "adding state track")
;(defonce logger (r/track! log-app-state))
 (add-watch state :track 
	(fn [_key _atom old-state new-state]
		(let [for-removal (apply dissoc old-state (keys new-state))
		      for-addition (apply dissoc new-state (keys old-state))]
		;(.log js/console "old-state: " old-state "new-state: " new-state)
		(.log js/console "for-removal: " (str for-removal) "for-addition: " (str for-addition))
		
		(doseq [[pos v] for-removal] (do (prn "removing" pos) (.setBlock game (clj->js (encode-coord pos)) 0)))
		(doseq [[pos v] for-addition] (do (prn "adding" pos v)(.setBlock game (clj->js (encode-coord pos)) (encode-color v))))
		))))

(defn apply-differences-to-game! [game old-state new-state]
		(let [for-removal (apply dissoc old-state (keys new-state))
		      for-addition (apply dissoc new-state (keys old-state))]
		;(.log js/console "old-state: " old-state "new-state: " new-state)
		(.log js/console "for-removal: " (str for-removal) "for-addition: " (str for-addition))
		(doseq [[pos v] for-removal] (.setBlock game (clj->js pos) 0))
		(doseq [[pos v] for-addition] (.setBlock game (clj->js pos) v))
		))

(defn setup-player-walking-animation! [game]
 (.on game "tick" (fn []
		   (let [target (get-target game)
		    velocity (.. target -velocity length)
		    is-moving? (< velocity 0.001)]
		    (.render voxel-walk (.-playerSkin target))
		    (if is-moving?
		     (.startWalking voxel-walk)
		     (.stopWalking voxel-walk))))))

(defn is-scenery? [[x y z]]
	(< y 63))

(defn remove-block! [game position]
 (let [encoded-position (encode-coord position)]
    (when-not (is-scenery? encoded-position)
	(.setBlock game encoded-position 0))))

(defn add-block! [game position block-type]
 (let [encoded-position (encode-coord position)]
  (when (inside-floor? encoded-position)
   (.log js/console encoded-position (encode-color block-type))
   (.setBlock game (clj->js encoded-position) (encode-color block-type)))))

(defn raycast-adjacent [game]
 (some-> game
  (.raycastVoxels)
  (.-adjacent)
  (decode-coord)))

(defn add-block-at-pointer! [game block-type] 
    (let [position (raycast-adjacent game)]
        (add-block! game position block-type)))

(defn add-to-dom [game el]
 (set! (.-container game) el)
 (.setDimensions game #js{:container el})
 (.onWindowResize game)
(.appendTo game el))

(defn setup-interaction! [game]
 (.on game "fire" (fn [target state] 
		   (let [position (.-voxel (.raycastVoxels game))]
		     (remove-block! game position)))))

(defn setup! [game state]
 (setup-highlight! game)
 (comment (prn "creating block" (.canCreateBlock game (clj->js [-5 63 -5])) (.createBlock game (clj->js [-2 63 -2]) 86)))
 (setup-interaction! game)
 (setup-player! game)
(listen-world-change game state)
; (setup-player-walking-animation! game)
)
