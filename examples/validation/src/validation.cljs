(ns examples.validation
  (:require [reforms.rum :include-macros true :as f]
            [reforms.validation :as v :include-macros true]
            [rum.core :as rum]
            [examples.shared.utils :refer [inspector-view]]
            [clojure.string :as str]))

(def customers (atom []))
(def customer (atom {:city "New York"}))
(def ui-state (atom {}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Custom validator

(defn positive-number?
  [s]
  (pos? (js/parseInt s)))

(defn positive-number
  [korks error-message]
  (v/is-true korks positive-number? error-message))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def customer-validators
  [(v/present [:first] "Enter first name")
   (v/present [:last] "Enter last name")
   (positive-number [:age] "Age must be a positive number")
   (v/present [:login] "Enter login name")
   (v/equal [:password1] [:password2] "Passwords do not match")
   (v/present [:password1] "Choose password")
   (v/present [:password2] "Re-enter password")])

(defn sign-up!
  [customers customer ui-state]
  (when (apply v/validate! customer ui-state customer-validators)
    (swap! customers (fn [xs] (conj xs @customer)))))

(defn force-error!
  [customer ui-state]
  (v/validate!
    customer
    ui-state
    (v/force-error [:server-error] "An error has occurred")))

(rum/defc signup-form-view < rum/cursored rum/cursored-watch
  [customers customer ui-state]
  (f/panel
    "Add customer"
    (f/with-options
      {:form-horizontal    true
       :label-column-class "col-md-4"
       :input-column-class "col-md-8"}
      (v/form ui-state
              {:on-submit #(sign-up! customers customer ui-state)}
              (v/text "First name"  customer [:first] :placeholder "Enter first name")
              (v/text "Last name" customer [:last] :placeholder "Enter last name")
              (v/text "City" customer [:city]
                      :warn-fn #(when-not (= "Kansas" %) "We're not in Kansas anymore") :placeholder "Where are you?")
              (v/number "Age" customer [:age] :placeholder "Enter your age")
              (v/text "Login" customer [:login] :placeholder "Choose login" )
              (v/password "Password" customer [:password1] :placeholder "Enter password")
              (v/password "Confirm password" customer [:password2] :placeholder "Re-enter password")
              (v/error-alert [:server-error])
              (f/form-buttons
                (f/button-primary "Save" #(sign-up! customers customer ui-state))
                (f/button-default "Simulate server error" #(force-error! customer ui-state)))))))

(rum/defc customer-list-view < rum/cursored rum/cursored-watch
  [customers]
  [:div.customers
   [:h3 "Customers"]
   (if (not-empty @customers)
     [:ul.list-unstyled
      (for [{:keys [first last]} @customers]
        [:li (str/join " " [first last])])]
     [:em "No customers."])])

(rum/defc main-view
  []
  [:div
   [:div.row
    [:div.col-md-4.col-md-offset-2 (signup-form-view customers customer ui-state)]
    [:div.col-md-4
     (customer-list-view customers)]]
   [:div.row
    [:div.col-md-8.col-md-offset-2
     (inspector-view customers)
     (inspector-view customer)
     (inspector-view ui-state)]]])

(rum/mount (main-view) (. js/document (getElementById "app")))
