(ns visitera.validation
  (:require [struct.core :as st]))

(def registr-schema
  [[:email st/required st/string st/email]
   [:password st/required st/string {:message "password should contains at least 8 characters"
                                     :validate #(> (count %) 7)}]])

(defn validate-register [params]
  (first (st/validate params registr-schema)))
