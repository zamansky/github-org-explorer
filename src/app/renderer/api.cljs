(ns app.renderer.api
  (:require
   [graphql-query.core :refer [graphql-query]]
   [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
   [cljs-http.client :as http]
   ))

(defn load-orgs-into-state [state]
  (prn state)
  (go (let [response (<! (http/get "https://api.github.com/user/orgs" {:with-credentials? false
                                                                       :headers {"Authorization" (str "Basic " (get @state  :credentials))}
                                                                       }))]

        (let [orglist (mapv #(:login %) (:body response))]
          (swap! state assoc :orgs orglist :org (first orglist) )
          ))))


