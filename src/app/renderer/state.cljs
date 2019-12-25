(ns app.renderer.state
  (:require [reagent.core :as r]
            [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
            [cljs-http.client :as http]
            [goog.crypt.base64 :refer [encodeString decodeString]]
            ))



(defonce state
  (r/atom {:authenticated false
           :username "username"
           :password "password"
           :orgs[]
           :org ""
           :all-repos []
           :active-repos []
           
           }))

(declare event-queue)

(defn login [state {:keys [username password] :as payload}]
  (prn username password)
  (let [credentials (encodeString (str username ":" password))]
    (go (let [response (<! (http/get "https://api.github.com/" {:with-credentials? false
                                                                :headers {"Authorization" (str "Basic " credentials)}
                                                                }))]
          (if (:success response)
            (>! event-queue [:succesful-login-completed {:username username :credentials credentials}])
            )
          )) 
    ))

(defn login-succeeded [state {:keys [username credentials] :as payload}]
  (swap! state assoc :authenticated true :username username :credentials credentials)
  )



(defn logout [state payload]
  (swap! state assoc :authenticated false :username nil :credentials nil))

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






