(ns app.renderer.core
  (:require [reagent.core :as r]
            [clojure.string :as string :refer [split-lines]]
            [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
            [app.renderer.state :as state]
            [app.renderer.api :as api]
            [taoensso.timbre :as timbre :refer [log info ]]
            ))

(def electron (js/require "electron"))
(def ipcRenderer (.-ipcRenderer electron))

(defn input-field [type name localstate field]
  [:div.flex
   [:label.block.text-tray-500.font-bold.md:.mb-2.mr-3.py-1 (str  name ": ")]
   [:input.appearance-none.h-1.block.bg-gray-200.border-2.border-gray200.rounded.px-2.py-4
    {:type type
     :on-change #(swap! localstate assoc-in [field] (-> % .-target .-value))
     :value (field @localstate) :id name} 
    ]
   ]
  )

(defn button-field [name action localstate]
  [:button.text-sm.bg-blue-500.hover:bg-blue-700.text-white.font-bold.px-3.rounded {:on-click #(action localstate) :id name} name]
  )
(defn org-selector []
  (let [orgs (:orgs @state/state)
        org ""]
    [:select {:on-change #(do (swap! state/state assoc :org (-> % .-target .-value))
                              (swap! state/state assoc :filter "")
                              (api/load-all-repos state/state)
                              )}
     
     (for [o orgs]
       ^{:key o}[:option {:value o} o]
       )
     ]
    
    ))



(defn navbar []
  (let [localstate (r/atom {:username "" :password ""})]
    (fn []
      
      [:ul.flex
       [:li.m-6.py-2.my-1 "Organization: " (org-selector)]
       
       ]
      
      )))

(defn filter-repos []
  (let [filt  (:filter @state/state)
        repos     (if (or (= filt "") (nil? filt))
                    (do (swap! state/state assoc :active-repos (:all-repos @state/state))
                        (:active-repos @state/state))
                    (do
                      (let [filtered  (into []  (filter #(try
                                                           (re-matches (re-pattern (str ".*" filt ".*")) %)
                                                           (catch js/Object e %))
                                                        (:all-repos @state/state)))
                            ]
                        (do
                          (swap! state/state assoc :active-repos filtered)
                          (:active-repos @state/state)
                          )
                        )
                      ))
        ]
    repos
    )
  )

(defn get-repo-list []
  (let [repos (filter-repos)
        org (:org @state/state)
        url (goog.string/format "https://github.com/%s/" org)
        
        ]
    [:ol
     (for [r repos]
       ^{:key r}[:li.hover:bg-blue-500.cursor-pointer [:a {:on-click #(do
                                                                        (prn (str url r))
                                                                        (.openExternal electron.shell (str url r "/")))} r]]
       )

     ]
    )
  )


(defn filter-input[]
  [:div.flex
   [:label.block.text-tray-500.font-bold.md:.mb-2.mr-3.py-1 "Filter: "]
   [:input.h-5.p-3.my-2.w-full
    {:on-change #(do
                   ( swap! state/state assoc :filter  (-> % .-target .-value))
                   
                   )
     :value (:filter @state/state)
     
     }
    ]
   ]
  )
(defn empty-modal[]
  [:div#modals])


(defn export-modal []
  (let [chop (api/find-common-prefix (:active-repos @state/state))
        pchop (if (= (last chop) \-) (apply str (drop-last chop)) chop)

        payload (r/atom {:chop chop
                         :path (str "/tmp/" pchop)
                         })
        ]

    (fn []
      [:div#modals.fixed.pin.z-50.overflow-auto.flex.h-full.w-full.bg-smoke-lightest.opacity-100
       
       [:div.relative.p-8.bg-full-white.w-full.max-w-md.m-auto.flex-col.flex
        [:label.block.text-tray-500.font-bold.md:.mb-2.mr-3.py-1 "Path: "]
        [:input.appearance-none.h-1.block.bg-gray-200.border-2.border-gray200.rounded.px-2.py-4
         {
          :type "text"
          :on-change #(swap! payload assoc-in [:path] (-> % .-target .-value))
          :value (:path @payload) :id "path"
          } 

         ]
        [:label.block.text-tray-500.font-bold.md:.mb-2.mr-3.py-1 "Chop: "]
        [:input.appearance-none.h-1.block.bg-gray-200.border-2.border-gray200.rounded.px-2.py-4
         {
          :type "text"
          
          :on-change #(swap! payload assoc-in [:chop] (-> % .-target .-value))
          :value (:chop @payload) :id "chop" 
          }

         ]
        [:button.bg-red-500.hover:bg-blue-700.text-white.font-bold.px-3..mx-4.my-1.rounded {:on-click #(r/render-component empty-modal (.getElementById js/document "modals"))}"Cancel"]
        [:button.bg-blue-500.hover:bg-blue-700.text-white.font-bold.px-3..mx-4.my-1.rounded
         {:on-click #(do
                       (api/export-repos @payload @state/state)
                       (r/render-component empty-modal (.getElementById js/document "modals")))
          }
         "Export"]
        ]

       ]
      )))

(defn delete-modal []
  (let [confirmbox (r/atom "")
        repos (:active-repos @state/state)
        num-repos (count repos)
        is-disabled (r/atom true)
        ]

    (fn []
      [:div#modals.fixed.pin.z-50.overflow-auto.flex.h-full.w-full.bg-smoke-lightest.opacity-100
       
       [:div.relative.p-8.bg-full-white.w-full.max-w-md.m-auto.flex-col.flex
        [:div [:span.text-red-600.font-bold "Danger - "] (goog.string/format "about to delete %d repos. Type " num-repos) [:span.font-bold.font-lg "DELETE"] " to confirm deletion." ]
        [:label.block.text-tray-500.font-bold.md:.mb-2.mr-3.py-1 "Confirm: "]
        [:input.appearance-none.h-1.block.bg-gray-200.border-2.border-gray200.rounded.px-2.py-4
         {
          :type "text"
          :on-change #(let [element (.getElementById js/document "delete-button")
                            cl  (.-classList element)
                            ]
                        (reset! confirmbox (-> % .-target .-value))
                        (if (= @confirmbox "DELETE")
                          (do  (reset! is-disabled false)
                               (.remove cl "bg-blue-500")
                               (.add cl "bg-green-500")
                               )
                          (do  (reset! is-disabled true)
                               (.remove cl "bg-green-500")
                               (.add cl "bg-blue-500")
                               )
                          ;; document.getElementById ("button").disabled=false
                          )
                        )
          :value @confirmbox :id "confirm"
          } 

         ]

        
        [:button.bg-red-500.hover:bg-blue-700.text-white.font-bold.px-3..mx-4.my-5.rounded {:on-click #(r/render-component empty-modal (.getElementById js/document "modals"))}"Cancel"]
        
        [:button#delete-button.bg-blue-500.hover:bg-blue-700.text-white.font-bold.px-3.mx-4.my-5.rounded
         {:disabled @is-disabled
          :on-click #(do
                       (api/delete-repos state/state)
                       (r/render-component empty-modal (.getElementById js/document "modals"))
                       (swap! state/state assoc :filter "")
                       (api/load-all-repos state/state)
                       )
          }
         "DELETE"]
        ]

       ]
      )))

(defn main-component []
  [:div#main
   [:div#modals]

   [:h1.m-5.text-4xl.font-bold "Organization Dashboard"]
   [:h2.m-2.px-2.text-4xl.font-bold.bg-red-600.rounded.max-w-sm.rounded (:status @state/state)]
   [navbar]
   [:hr]
   (if (:authenticated @state/state)
     [:div.flex
      [:div {:class "w-1/4"}
       (filter-input)
       [:button.bg-blue-500.hover:bg-blue-700.text-white.font-bold.px-3..mx-4.my-1.rounded {:on-click #(r/render-component [export-modal] (.getElementById js/document "modals") )} "Export"]
       [:button.bg-blue-500.hover:bg-blue-700.text-white.font-bold.px-3..mx-4.my-1.rounded {:on-click #(r/render-component [delete-modal] (.getElementById js/document "modals") )} "delete"]
       [:button.bg-green-500.hover:bg-green-700.text-white.font-bold.px-3..mx-4.my-4.rounded {:on-click #(api/load-all-repos state/state )} "reload"]
       ]
      [:div.px-3 {:class "w-3/4"} (get-repo-list)]
      ])
   ]

  )


(defn start! []
  (.on ipcRenderer "token" #((do
                               (swap! state/state assoc :token (js->clj %2))
                               (app.renderer.api/load-orgs-into-state state/state)
                               (swap! state/state assoc :authenticated true)
                               ) ) )
  (.send ipcRenderer "get-token" "")
  (r/render
   [main-component]
   (js/document.getElementById "app-container")))

(start!)

