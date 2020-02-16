(ns app.renderer.state
  (:require [reagent.core :as r]
            [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
            [cljs-http.client :as http]
            [goog.crypt.base64 :refer [encodeString decodeString]]
            [app.renderer.api]
            ))


(def original-state {:authenticated false
                     :token ""
                     :username "username"
                     :password "password"
                     :orgs[]
                     :org ""
                     :filter ""
                     :all-repos []
                     :active-repos []
                     :status ""
                     })
(def state
  (r/atom original-state))
(declare event-queue)

(defn login [state {:keys [username password] :as payload}]
  (let [credentials (encodeString (str username ":" password))]
    (go (let [response (<! (http/get "https://api.github.com/user/orgs" {:with-credentials? false
                                                                         :headers {"Authorization" (str "Basic " credentials)}
                                                                         }))]
          (if (:success response)
            (>! event-queue [:succesful-login-completed {:username username :password password :credentials credentials}])
            (swap! state assoc :status "Invalid login")
            )
          )) 
    ))

(defn login-succeeded [state {:keys [username credentials password] :as payload}]
  (swap! state assoc :authenticated true :username username :password password :credentials credentials :status "")
  (app.renderer.api/load-orgs-into-state state)
  )



(defn logout [state payload]
  (reset! state original-state))

(def event-queue (chan))

(def event-map {:login login
                :succesful-login-completed login-succeeded
                :logout logout})

;; process events)
(go
(while true
  (let [[event payload] (<! event-queue)]
    ((event event-map) state payload)
    )))






