(ns app.renderer.api
  (:require
   [cljs.core.async :refer (chan put! <! >! go go-loop timeout)]
   [cljs-http.client :as http]
   [graphql-builder.parser :refer-macros [defgraphql]]
   [graphql-builder.core :as core]
   ))

(defgraphql graphq-queries "queries.graphql")
(def qmap (core/query-map graphq-queries))

(def z (get-in qmap [:query :load-orgs]))
;;(z {:username "zamansky"})




;;  :edges  [:cursor :node [:name]]]]]]})
