(ns tour.core
  (:use korma.db))

(defdb db (postgres {:db "tour"
                     :user "postgres"
                     :password "postgres"
                     :host "192.168.0.249"}))


