(ns app.renderer.api
  (:require
   [graphql-query.core :refer [graphql-query]]
   ))

(graphql-query  {:queries [[:user {:login "zamansky"}
                            [:organizations {:first 99}
                             [:edges  [cursor :node [:name]]]]]]})


;; query { 
;;        user(login:"zamansky"){
;;                               organizations(first:99){

;;       	                                              edges{
;;                                                             cursor

;;                                                             node{

;;                                                                  name 
;;                                                                  }
;;                                                             }

;;                                                       }

;;                               }

;;        }
