(ns app.main.keys)

(def client_id "")
(def client_secret "")
(def scope "repo admin:org delete_repo")
(def auth-url-base "https://github.com/login/oauth/authorize?client_id=")
( def auth-url (str auth-url-base client_id "&scope=" scope))
