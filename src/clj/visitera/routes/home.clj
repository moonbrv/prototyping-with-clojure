(ns visitera.routes.home
  (:require
    [visitera.layout :as layout]
    [clojure.java.io :as io]
    [visitera.middleware :as middleware]
    [ring.util.http-response :as response]
    [visitera.db.core :as dbcore]
    [visitera.validation :refer [validate-register validate-login]]
    [buddy.hashers :as hs]
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

(defn password-valid? [user password]
  (hs/check password (:user/password user)))

(defn login-handler [{:keys [params session]}]
  (if-let [errors (validate-login params)]
    (-> (response/found "/login")
        (assoc :flash {:errors errors
                       :email  (:email params)}))
    (if-let [user (dbcore/find-user (d/db dbcore/conn) (:email params))]
      (cond
        (not user)
        (-> (response/found "/login")
            (assoc :flash {:errors {:email "User with this email does not exist"}
                           :email  (:email params)}))

        (and user (not (password-valid? user (:password params))))
        (-> (response/found "/login")
            (assoc :flash {:errors {:password "Password is Invalid!"}
                           :email  (:email params)}))

        (and user (password-valid? user (:password params)))
        (let [updated-session (assoc session :identity (-> params :email keyword))]
          (-> (response/found "/")
              (assoc :session updated-session)))))))

(defn logout [req]
  (-> (response/found "/login")
      (assoc :session {})))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get layout/home-page}]
   ["/login" {:get layout/login-page
              :post login-handler}]
   ["/logout" {:get logout}]
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

