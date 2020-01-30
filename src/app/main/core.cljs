(ns app.main.core
  (:require ["electron" :refer [app BrowserWindow crashReporter ipcMain ]]
            [app.main.keys :as k]
            [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
            [cljs-http.client :as http]
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
         url "https://github.com/login/oauth/access_token"
         ]
    (go (let [response
              (<! (http/get url {:query-params {"client_id" k/client_id "client_secret" k/client_secret "code" code }
                                 :headers {"Accept" "application/json"}
                                 }))
              ]
          (reset! token (extract-token (:body response)))
          #_(prn @token)
          (.loadURL @auth-window (str "file://" js/__dirname "/public/index.html"))
          
          ))))




;;-------------------------------------------
;; up to init browser

(defn init-browser []
  (reset! main-window (BrowserWindow.
                       (clj->js {:width 1024
                                 :height 768
                                 :webPreferences {:nodeIntegration true}

                                 })))
                                        ; Path is relative to the compiled js file (main.js in our case)
  (.loadURL @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on @main-window "closed" #(reset! main-window nil)))


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
