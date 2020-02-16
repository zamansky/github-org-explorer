(ns app.main.keys)

(def client_id "3056f749c2e91ce4d780")
(def client_secret "8dbd436a98a63894a7408f811ea764ce8a6ad0ff")
(def scope "repo read:org user admin:org delete_repo")
(def auth-url-base "https://github.com/login/oauth/authorize?client_id=")
( def auth-url (str auth-url-base client_id "&scope=" scope))
