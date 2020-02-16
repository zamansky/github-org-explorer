(ns app.main.core
  (:require ["electron" :refer [app BrowserWindow crashReporter ipcMain ]]
            [app.main.keys :as k]
            [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
            [cljs-http.client :as http]
            [ajax.core :refer [GET POST]]
            
            ))



(def main-window (atom nil))
(def auth-window (atom nil))
(def token (atom ""))

;;------------------------------------------
(defn extract-code [url]
  (second  (re-matches #".*code=([0-9a-f]+).*" url))
  )

(defn extract-token [url]
  (second  (re-matches #".*token=([0-9a-f]+).*" url)))


(.on ipcMain "get-token"
     #(.reply %1 "token" @token)
     )

(defn get-token [url]
  (let  [code (extract-code url)
         ghurl "https://github.com/login/oauth/access_token"
         ]

    (go (let [response
              (<! (http/get ghurl {:query-params {"client_id" k/client_id "client_secret" k/client_secret "code" code }
                                   :headers {"Accept" "application/json"}
                                   }))
              ]
          (prn "THERESPONSE" )
          (.log js/console  "THERESPONSE")
          (reset! token (extract-token (:body response)))
          (prn @token)
          (.loadURL @auth-window (str "file://" js/__dirname "/public/index.html"))
          
          ))))




;;-------------------------------------------
;; up to init browser
(defn init-browser []
  (reset! auth-window (BrowserWindow.
                       (clj->js {:width 1200
                                 :height 1200
                                 :webPreferences {:nodeIntegration true}
                                 ;;"node-integration" false
                                 ;;"web-security" false
                                 })))

  ;;(.show @auth-window)
  (.catch (.loadURL @auth-window k/auth-url)
          (fn [x]
            (prn "in catch" x)
            (get-token (.toString  (js->clj x)))
            ;;(.log js/console  x))
            ))

  ;;(.webContents.on @auth-window "did-fail-load" #(.log js/console (.sender.history %)))
  (.on @auth-window "load" #(prn %1))
  (.on @auth-window "closed" #(reset! main-window nil))
  (.webContents.on @auth-window  "will-navigate" #(do
                                                    (prn "HELLO ONwebcobtebts")
                                                    (prn  %1 %2  )
                                                    ( get-token %2))))

;;----------------------------------------------------

(defn main []
                                        ; CrashReporter can just be omitted
  (.start crashReporter
          (clj->js
           {:companyName "MyAwesomeCompany"
            :productName "MyAwesomeApp"
            :submitURL "https://example.com/submit-url"
            :autoSubmit false}))

  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-browser))
