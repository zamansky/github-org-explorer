(ns app.renderer.api
  (:require
   [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
   [cljs-http.client :as http]
   ))

(defn load-orgs-into-state [state]
  (go (let [response
            (<! (http/get "https://api.github.com/user/orgs" {:with-credentials? false
                                                              :headers {"Authorization" (str "Basic " (get @state  :credentials))}
                                                              }))]
        (let [orglist (mapv #(:login %) (:body response))]
          (swap! state assoc :orgs orglist :org (first orglist) )
          ))))

(defn load-all-repos [state]
  (let [org (:org @state)
        creds (:credentials @state)
        url (goog.string/format "https://api.github.com/orgs/%s/repos" org)
        ]
    (go
      (let [response (<! (http/get url {:with-credentials? false
                                        :headers {"Authorization"
                                                  (goog.string/format "Basic %s" creds)}}))]
        (let [l (get (:headers response) "link")]
          (prn "THE LINK: " l)
          )
        )
      )
    )
  )

