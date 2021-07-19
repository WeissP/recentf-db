(def db
  {:dbtype "postgresql",
   :dbname "recentf",
   :host "127.0.0.1",
   :port 5432,
   :user "weiss",
   :password ""})

(def ds (pg/get-connection db))

