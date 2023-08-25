(ns minecraft-web.app
 (:require 
  [minecraft-web.util :as util]
  [minecraft-web.game :as game]))

(defn ^:dev/before-load stop []
  (js/console.log "stop"))

(defn ^:dev/after-load start []
  (js/console.log "start"))

(defn init []
  (println "Hello World"))

(let [g (game/create)
 container (.appendChild (util/$ "div#root") (util/createEl "div"))]
(game/add-to-dom g container)
(game/setup! g)
;(prn (.playerPosition game))
  
 )
