(ns tour.handler
  (:require [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :refer [run-jetty]]      
            [compojure.core :refer [defroutes ANY]]
            [clojure.data.json :as json]
            [tour.backend :as b]))

(defn value-writer
  "Helper function to transform the timestamps to the correct string"
  [key value]
  (cond
   (= (type value) java.sql.Timestamp) (b/date-to-string value)
   :else value))

(defmulti to-tour 
;;  "Multimethod to create the retured json map/list" 
  class)

(defmethod to-tour
;;  "Multimethod to create the retured json map"
  clojure.lang.PersistentHashMap
  [tour]
  (-> tour
      (assoc :geometry (map #(dissoc % :tour_id :id) (:waypoint tour)))
      (assoc :creator (:name (b/get-user-by-id (:user_id tour))))
      (dissoc :user_id :waypoint)))


(defmethod to-tour
;;  "Multimethod to create the retured json list"
  clojure.lang.LazySeq
  [tours]
  (map #(to-tour %) tours))
  

(defresource get-tour 
;;  "Returns the tour to the given id"
  [id]
  :available-media-types ["application/json"]
  :allowed-methods [:get]
  :handle-ok (fn [_]
               (-> (b/get-tour id)
                   (to-tour)
                   (json/write-str :value-fn value-writer :key-fn name))))

(defresource get-tours-by-username
 ;; "Returns all tours from the given username"
  [username]
  :available-media-types ["application/json"]
  :allowed-methods [:get]
  :handle-ok (fn [_]
               (-> (b/get-tours-by-username username)
                   (to-tour)
                   (json/write-str :value-fn value-writer :key-fn name))))

(defresource get-tours-by-startpoint
;;  "Returns a list with all matching tours. A tour is matching if one tour point is 
;;   in the searched area"
  [lat long radius]
  :available-media-types ["application/json"]
  :allowed-methods [:get]
  :handle-ok (fn [_]
               (-> (b/get-tours-by-startpoint lat long radius)
                   (to-tour)
                   (json/write-str :value-fn value-writer :key-fn name))))

(defresource get-tours-by-startpoint-filtered-by-sport
;;  "Returns a list with all matching tours. A tour is matching if one tour point is 
;;   in the searched area and if the sport is equal"
  [lat long radius sport]
  :available-media-types ["application/json"]
  :allowed-methods [:get]
  :handle-ok (fn [_]
               (-> (b/get-tours-by-startpoint-filtered-by-sport lat long radius sport)
                   (to-tour )
                   (json/write-str :value-fn value-writer :key-fn name))))


(defresource save-tour
;;  "Save a list of tours or only one tour. If the tour has no id field the a new id is created at upload.
;;  If a tour with the given id exists, it will be removed before the new one is inserted."
  []
  :available-media-types ["application/json"]
  :allowed-methods [:post]
  :post! (fn [ctx]
               (let [body (slurp (get-in ctx [:request :body]))]
                 (b/convert-tour-json body))))


(defroutes app
;;  "All routes together"
  (ANY "/api/tour/" [] (save-tour))
  (ANY "/api/tour/:id" [id] 
       (get-tour (read-string id)))
  (ANY "/api/:username/tours" [username] 
       (get-tours-by-username username))
  (ANY "/api/:lat/:long/:radius/tours" [lat long radius] 
       (get-tours-by-startpoint (read-string lat) (read-string long) (read-string radius)))
  (ANY "/api/:lat/:long/:radius/:sport/tours" [lat long radius sport] 
       (get-tours-by-startpoint-filtered-by-sport (read-string lat) (read-string long) (read-string radius) sport)))

(def handler
  (-> app
      (wrap-params)))
