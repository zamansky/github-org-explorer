(ns app.renderer.api
  (:require
   [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
   [cljs-http.client :as http]
   [taoensso.timbre :as timbre :refer [log info ]]
   ))

(defonce simple-git (js/require "simple-git"))
(defonce git (simple-git "/"))

(defn load-orgs-into-state [state]
  (go (let [response
            (<! (http/get "https://api.github.com/user/orgs" {:with-credentials? false
                                                              :headers {"Authorization" (str "Bearer " (get @state :token) )}})) 
            ]
        (let [orglist (into [""] (mapv #(:login %) (:body response)))]
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
        token (:token @state)
        url (goog.string/format "https://api.github.com/orgs/%s/repos" org)
        ]
    (swap! state assoc :all-repos ["LOADING"])
    (go-loop [repos [] url url ]
      (let [response (<! (http/get url {:with-credentials? false
                                        :headers {"Authorization"(goog.string/format "Bearer %s" token)}}))
            body (:body response)
            headers (:headers response)
            link (get headers "link")
            additional-repos (map #(:name %) body)
            next-link (if (not (nil? link)) (parse-link link) nil)
            ]
        (cond
          (nil? link)  (swap! state assoc :all-repos (sort (into repos additional-repos )))
          
          :else (recur (into repos additional-repos) next-link ))
        )
      )
    
    )
  )



(defn find-common-prefix [l]
  ( reduce (fn f [a b]
             (->> (mapv vector a b)
                  (take-while (fn [[a b]] (= a b)))
                  (map first)
                  (apply str)
                  )) l))

(defn export-repos [{:keys [chop path] :as payload} {:keys [active-repos] :as state}]
  (let [base-url (goog.string/format "https://%s@github.com/%s/" (:token state)  (:org state))
        
        ]
    (doseq [repo active-repos]
      (let [url (goog.string/format "%s/%s" base-url repo)
            stripped-reponame (clojure.string/replace repo chop "")
            dest (str path "/" stripped-reponame)
            ]
        (.clone git url dest (fn [r] (info "Cloned: " repo " - "  r)))

        ) )))


;;DELETE https://api.github.com/repos/zamansky/graphql-test
(defn delete-repos [state]
  (doseq [repo (:active-repos @state)]
    (info "Deleting: " repo)
    (go (let [org (:org @state)
              credentials (:credentials @state)
              token (:token @state)
              url (goog.string/format "https://api.github.com/repos/%s/%s" org repo)
              response (<! (http/delete url
                                        {:with-credentials? false
                                         :headers {"Authorization" (str "Bearer " token)}}))
              ]
          (if (:success response)
            (do (info "Deleted " repo)
                )
            (info "Error in deleting " repo " : " (:status response))
            )
          
          )))

  )
