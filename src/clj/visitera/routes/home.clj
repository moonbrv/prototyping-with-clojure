(ns visitera.routes.home
  (:require
    [visitera.layout :as layout]
    [clojure.java.io :as io]
    [visitera.middleware :as middleware]
    [ring.util.http-response :as response]
    [visitera.db.core :as dbcore]
    [visitera.validation :refer [validate-register]]
    [datomic.api :as d]))

(defn register-handler! [{:keys [params]}]
  (if-let [errors (validate-register params)]
    (-> (response/found "/register")
        (assoc :flash {:errors errors
                       :email  (:email params)}))
    (if-not (dbcore/add-user dbcore/conn params)
      (-> (response/found "/register")
          (assoc :flash {:errors {:email "User with this email already exist"}
                         :email  (:email params)}))
      (-> (response/found "/login")
          (assoc :flash {:messages {:success "You are successfuly registered"}
                         :email    (:email params)})))))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get layout/home-page}]
   ["/register" {:get  layout/register-page
                 :post register-handler!}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/db-test" {:get (fn [_]
                       (let [db (d/db dbcore/conn)
                             user (dbcore/find-user db "abc")]
                         (-> (response/ok (:user/name user))
                             (response/header "Content-Type" "text/plain; charset=utf-8"))))}]])

