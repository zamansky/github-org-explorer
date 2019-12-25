(ns app.renderer.core
  (:require [reagent.core :as r]
            [clojure.string :as string :refer [split-lines]]
            [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
            [app.renderer.state :as state]
            ))


(enable-console-print!)

(defn login [payload]
  (put! state/event-queue [:login @payload])
  )

(defn logout [payload]
  (put! state/event-queue [:logout @payload])
  )

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

(defn navbar []
  (let [localstate (r/atom {:username "" :password ""})]
    (fn []
      (if (not  (:authenticated @state/state))
        [:ul.flex
         [:li.mr-6 (input-field "text" "Username" localstate :username)]
         [:li.mr-6 (input-field "text" "Password" localstate :password)]
         [:li.mr-6 (button-field  "Login" login localstate)]
         ]
        [:ul.flex   [:li.m-6.py-2.my-1 (button-field "Logout" logout localstate)]]
        )
      )))

(defn main-component []
  [:div 
   [:h1.m-5.text-4xl.font-bold "Organization Dashboard"]
   [:div.py-1.font-bold "Login with your GitHub ID"]
   [navbar]
   [:hr]
   ]

  )


(defn start! []
  (r/render
   [main-component]
   (js/document.getElementById "app-container")))

(start!)

