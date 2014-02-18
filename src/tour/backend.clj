(ns tour.backend
  (:require [clojure.data.json :as json]
            [clj-time.format :as f]
            [clj-time.coerce :as c])
  (:use komoot.core
        korma.db
        korma.core))

;; Definitions for the korma sql helper

(defentity waypoints
  (pk :id)
  (table :waypoint)
  (database db))

(defentity tours
  (pk :id)
  (table :tour)
  (has-many waypoints)
  (database db))

(defentity users
  (pk :id)
  (table :user)
  (has-many tours :creator)
  (database db))


(defn get-user 
  "returns the corresponding user object to the given name"  
  [username]
  (first
   (select users
           (where {:name username})
           (limit 1))))


(defn get-user-by-id 
  "returns the corresponding user object to the given id"
  [id]
  (first
   (select users
           (where {:id id})
           (limit 1))))

(defn add-user
  "adding the given user name to the database. If it not exists."
  [username]
  (if-let [user (get-user username)]
    user
    (insert users
            (values {:name username}))))


(defn add-tour
  "Adds a complete tour object. (Including the waypoints)
   Checks if the tour as an id. If the tour has no id, a new id is 
   created by the database. If there is a id. The tour with the given id 
   will be removed before the new tour object is added to the database"
  [tour user_id ]
  (let [tour_val    {:user_id user_id
                     :name (:name tour)
                     :recordSource (:recordSource tour)
                     :sport (:sport tour)
                     :status (:status tour)
                     :trackSourceDevice (:trackSourceDevice tour)
                     :altDiff (:altDiff tour)
                     :altDown (:altDown tour)
                     :changedAt (:changedAt tour)
                     :createdAt (:createdAt tour)
                     :distance (:distance tour)
                     :duration (:duration tour)
                     }
        
        tour_values (if (contains? tour :id)
                      (do
                        (delete tours
                                (where {:id (:id tour)}))
                        (assoc tour_val :id (:id tour)))
                      tour_val)
        
        tour_row (insert tours
                         (values tour_values))]
  (doall
   (map #(insert waypoints
                 (values {:tour_id (:id tour_row)
                          :time (:time %) 
                          :x (:x %)
                          :y (:y %)
                          :z (:z %)
                          }))
        (:geometry tour)))))

(defmulti import-tour 
;;  "Multimethod to import tour objects and tour lists"
  class)

(defmethod import-tour clojure.lang.PersistentHashMap
;;  "Multimethod to import tour objects. Checks if the tour is known.
;;   If the user is not part of the database, he will be added."
  [tour]
  (let [user (add-user (:creator tour))]
    (add-tour tour (:id user))))

(defmethod import-tour clojure.lang.PersistentVector
;;  "Multimethod to import tour lists"
  [tours]
   (doall (map #(import-tour %) tours)))

(def date-format (f/formatter "YYYY-MM-dd HH:mm:ss +SSSS"))

(defn as-date [date-field]
  "Helper function for datetime transformation.
   json datetime to sql datetime"
  (c/to-sql-time (f/parse date-format date-field )))

(defn date-to-string [date]
  "Helper function for datetime transformation.
   sql datetime to json datetime"
  (f/unparse date-format (c/from-sql-time date )))

(defn date-aware-value-reader [key value]
  "JSON converter helper function to tranform some special 
   fields correct."
  (cond
   (= "NaN" value) nil
   (= key :changedAt) (as-date value)
   (= key :createdAt) (as-date value)
   (= key :time) (as-date value)
   :else value))

(defn convert-tour-json
  "Converts a tour json string to a tour clojure object/list"
  [tour-json]
  (transaction
   (import-tour (json/read-str tour-json 
                               :value-fn date-aware-value-reader
                               :key-fn keyword))))



(defn get-tour
  "Loads the complete tour, with waypoints, matching the given id"
 [id]
 (first 
  (select tours
          (with waypoints)
          (where {:id id})
          (limit 1))))

(defn get-tours-by-username
  "Loads all tours from the given username"
  [username]
  (if-let [user (get-user username)]
    (select tours
            (with waypoints)
            (where {:user_id (:id user)}))))

(defn get-tours-by-startpoint
  "Load all tours from the given circle area. Startpoint and radius"
  [x y radius]
  (let [tours (exec-raw ["select distinct tour_id from waypoint	 
                          where ST_DWithin(Geography(ST_MakePoint(x,y)), 
                          Geography(ST_MakePoint(?,?)),?);" [x y radius]] :results)] 
  (map #(get-tour (:tour_id %)) tours)))

(defn get-tours-by-startpoint-filtered-by-sport
  "Load all tours from the given circle area and matching sport."
  [x y radius sport]
  (let [tours (exec-raw ["select distinct tour_id, sport from waypoint, tour
	                  where tour.id = waypoint.tour_id and
	                        tour.sport = 'mtb' and
                                ST_DWithin(Geography(ST_MakePoint(x,y)), 
                                Geography(ST_MakePoint(?,?)),?);" [x y radius]] :results)] 
  (map #(get-tour (:tour_id %)) tours)))





