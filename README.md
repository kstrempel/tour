# Tour

A [Clojure](http://clojure.org/) experiment with [Liberator](http://clojure-liberator.github.io/liberator/), [Korma](http://sqlkorma.com/) [Postgis](http://postgis.net/) and [leiningen](http://leiningen.org/)

## Usage

First restore the database schema from ```tour-backup.backup``` to your postgis database. 

After restoring change the connection settings in core.clj

```
(def db (postgres 
           {:db "tour"
            :user "postgres" 
            :password "<password>"
            :host "<hostname>"}))
```

Start the server with

```
lein ring server-headless
```

Import the example json file

```
curl -X POST  -H "Content-Type: application/json" --data @test/kai.json http://localhost:5001/api/tour/
```

## Example REST calls

```
curl http://localhost:5001/api/tour/21396

curl http://localhost:5001/api/7.70537/51.9598/20/tours

curl http://localhost:5001/api/7.70537/51.9598/2000000/jogging/tours

curl http://localhost:5001/api/kai/tours
```

## License

Copyright © 2013 Kai Strempel

Licensed under the EPL (see the file epl.html).
