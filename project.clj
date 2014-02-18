(defproject tour "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"] 
                 [korma "0.3.0-RC6"]
                 [clj-time "0.6.0"]
                 [Liberator "0.10.0"]
                 [compojure "1.1.3"]
		 [ring/ring-core "1.2.1"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [org.postgresql/postgresql "9.2-1002-jdbc4"]]
  :plugins [[lein-ring "0.8.8"]
            [lein-marginalia "0.7.1"]]
  :ring { :handler tour.handler/app
          :port 5001}
  :profiles
  {:dev 
   {:dependencies [[javax.servlet/servlet-api "2.5"]
                   [ring-mock "0.1.5"]]}})

