(ns app.renderer.api
  (:require
   [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
   [cljs-http.client :as http]
   ))

(defn load-orgs-into-state [state]
  (go (let [response
            (<! (http/get "https://api.github.com/user/orgs" {:with-credentials? false
                                                              :headers {"Authorization" (str "Basic " (get @state  :credentials))}}))
            ]
        (let [orglist (mapv #(:login %) (:body response))]
          (swap! state assoc :orgs orglist :org (first orglist) )
          ))))

(defn parse-link [link]
  (let [re1 #".*<(https://api.github.com/organizations/[0-9]+/repos\?page=[0-9]+).*rel=\"next\".*"
        result (re-matches re1 link )
        ]
    (second  result)))
;; 
;;<https://api.github.com/organizations/54550700/repos?page=2>; rel=\"next\", <https://api.github.com/organizations/54550700/repos?page=18>; rel=\"last\""
(defn load-all-repos [state]
  (let [org (:org @state)
        creds (:credentials @state)
        url (goog.string/format "https://api.github.com/orgs/%s/repos" org)
        ]
    (go-loop [repos [] url url ]
      (let [response (<! (http/get url {:with-credentials? false
                                        :headers {"Authorization"(goog.string/format "Basic %s" creds)}}))
            body (:body response)
            headers (:headers response)
            link (get headers "link")
            additional-repos (map #(:name %) body)
            next-link (if (not (nil? link)) (parse-link link) nil)
            ]
        (cond
          (nil? link)  (swap! state assoc :all-repos (sort (into repos additional-repos )))
          
          :else (recur (into repos additional-repos) next-link)
          )
        )
      )
    )
  )


