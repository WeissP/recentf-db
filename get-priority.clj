(require '[babashka.pods :as pods])
(pods/load-pod 'org.babashka/postgresql "0.0.7")

(require '[pod.babashka.postgresql :as pg])

(load-file "/home/weiss/clojure/recentf-db/common.clj")

(defn parse-path
  [fullpath]
  (let [[filepath tramp_path tramp_type] (reverse (clojure.string/split fullpath
                                                                        #":"))]
    [filepath (or tramp_path "host") (or tramp_type "host")]))

(defn get-priority
  [fullpath]
  (->
    (pg/execute-one!
      ds
      (into
        ["SELECT priority FROM file WHERE filepath = ?  AND tramp_path = ?  AND tramp_type = ?;"]
        (parse-path fullpath)))
    :file/priority))

(if (= (count *command-line-args*) 1)
  (println (apply get-priority *command-line-args*))
  (println "args are wrong"))




