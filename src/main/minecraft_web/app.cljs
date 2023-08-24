(ns minecraft-web.app
 (:require [voxel-engine]
  [voxel-player]
  [medley.core :as m]
  [voxel-fly]
  [voxel-walk]
  [voxel-highlight :as highlight]
  [goog.events.KeyCodes :as keycodes]
  [goog.events :as gev])
 (:import [goog.events KeyEvent KeyHandler]))

(defn ^:dev/before-load stop []
  (js/console.log "stop"))

(defn ^:dev/after-load start []
  (js/console.log "start"))

(def $ #(.querySelector js/document %))
(def createEl #(.createElement js/document %))

(defn init []
  (println "Hello World"))


(def TEXTURE_PATH "https://cdn.jsdelivr.net/gh/snyxan/myWorld@main/textures/")

(def MATERIAL_MAP [ "blocks/stone" "blocks/stone_granite" "blocks/stone_granite_smooth" "blocks/stone_diorite" "blocks/stone_diorite_smooth" "blocks/stone_andesite" "blocks/stone_andesite_smooth" ["blocks/grass_top" "blocks/dirt" "blocks/grass_side"] "blocks/dirt" "blocks/coarse_dirt" ["blocks/dirt_podzol_top" "blocks/dirt_podzol_side"] "blocks/cobblestone" "blocks/planks_oak" "blocks/planks_spruce" "blocks/planks_birch" "blocks/planks_jungle" "blocks/planks_acacia" "blocks/planks_oak" "blocks/sapling_oak" "blocks/sapling_spruce" "blocks/sapling_birch" "blocks/sapling_jungle" "blocks/sapling_acacia" "blocks/sapling_oak" "blocks/bedrock" "blocks/water_still" "blocks/water_flow" "blocks/sand" "blocks/red_sand" "blocks/gravel" ["blocks/log_oak_top" "blocks/log_oak"] ["blocks/log_spruce_top" "blocks/log_spruce"] ["blocks/log_birch_top" "blocks/log_birch"] ["blocks/log_jungle_top" "blocks/log_jungle"] "blocks/leaves_oak" "blocks/leaves_spruce" "blocks/leaves_birch" "blocks/leaves_jungle" [ "blocks/sandstone_top" "blocks/sandstone_bottom" "blocks/sandstone_normal" ] "blocks/sandstone_smooth"  "blocks/sandstone_smooth" "blocks/deadbush" ["blocks/grass_top" "blocks/grass_side"] "blocks/fern" "blocks/deadbush" "blocks/wool_colored_white" "blocks/wool_colored_orange" "blocks/wool_colored_magenta" "blocks/wool_colored_light_blue" "blocks/wool_colored_yellow" "blocks/wool_colored_lime" "blocks/wool_colored_pink" "blocks/wool_colored_gray" "blocks/wool_colored_silver" "blocks/wool_colored_cyan" "blocks/wool_colored_purple" "blocks/wool_colored_blue" "blocks/wool_colored_brown" "blocks/wool_colored_green" "blocks/wool_colored_red" "blocks/wool_colored_black" "blocks/flower_dandelion" "blocks/flower_tulip_red" "blocks/mushroom_brown" "blocks/mushroom_red" "blocks/hardened_clay_stained_lime" "letter_r" "n_n" "o_n" "r_n" "t_n" "h_n" "s_s" "o_s" "u_s" "t_s" "h_s" "e_e" "a_e" "s_e" "t_e" "w_w" "e_w" "s_w" "t_w" "wireframed_blue" "wireframed_yellow" "wireframed_green" "wireframed_orange" "wireframed_purple" "wireframed_red" ])

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

(defn generate-basic-map [x y z] 
  (let [map-coord (get map-coords [x y z])
	too-high? (>= y 63)
	inside-floor? (and (<= -5 x 5) (<= -5 z 5))
	is-even? (even? (+ x z))]
	(cond 
	  too-high? 0
	  (some? map-coord) map-coord
	  (not inside-floor?) 53
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

;(.remove container)

(defn on-keypress [keycode-map]
	(let [key-handler (KeyHandler. js/window)
      	      press-fn (fn [key-press]
			(.log js/console key-press)
		      (when-let [f (get keycode-map (.. key-press -keyCode))]
					(f))
)]
	(gev/listen key-handler "key" press-fn)))
(.addEventListener js/window "keypress" #(.log js/console %))

(defn create-game 
	([opts] (voxel-engine. (clj->js (merge default-opts opts))))
	([] (voxel-engine. {})))

(let [highlight-position (atom nil)
game (voxel-engine. (clj->js default-opts))
	highlighter (highlight game #js{:color 0xff0000})
 container (.appendChild ($ "div#root") (createEl "div"))
 player ((voxel-player game) SPEAKER_SKIN_PATH)
 add-block-at-pointer (fn [block-type] (.log js/console "adding block") (.createAdjacent game (.raycastVoxels game) block-type))
 target (.. game -controls target)]
(on-keypress (m/map-kv-vals #(add-block-at-pointer %) {keycodes/NUM_ONE 86
keycodes/NUM_TWO 87
keycodes/NUM_THREE 88
keycodes/NUM_FOUR 89
keycodes/NUM_FIVE 90
keycodes/NUM_SIX 91}))
(.on highlighter "highlight" #(reset! highlight-position %))
	(.appendTo game container)
 (.possess player)
 ((.. player -yaw -position -set) 2 14 4)
 ;(.. player -yaw -position fromArray #js[2 14 4])
 (.set player.position 0 63 0);world-origin
 (.toggle player)
 (prn target)
 (.startFlying ((voxel-fly game) target))
 (.control game player)
 
(prn (.playerPosition game))
;(.setInterval js/window (fn [] (prn (.playerPosition game))) 1000)
(.on game "data" #(.log js/console %))
 (.on game "tick" (fn []
	(let [velocity (.. target -velocity length)
		is-moving? (< velocity 0.001)]
	(.render voxel-walk (.-playerSkin target))
	(if is-moving?
		(.startWalking voxel-walk)
		(.stopWalking voxel-walk))
	)))

  (.on game "fire" (fn [target state] (when (some? @highlight-position) (.createBlock game @highlight-position 1))))
   ; on keypress
  
 )
;(do 
;	(.possess player)
	;(.. player -yaw -position set 2 14 4)
	;(.. player -position set 0 63 0)
	;(.. player toggle))
;)

