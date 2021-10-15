;; (def *command-line-args* '("/home/weiss/.config/nyxt/init.lisp"))

(require '[babashka.pods :as pods])
(pods/load-pod 'org.babashka/postgresql "0.0.7")

(require '[pod.babashka.postgresql :as pg])

(load-file "/home/weiss/clojure/recentf-db/common.clj")
(load-file "/home/weiss/clojure/recentf-db/black_list.clj")

(defn parse-path
  [fullpath]
  (let [[filepath tramp_path tramp_type] (reverse (clojure.string/split fullpath
                                                                        #":"))]
    [filepath (or tramp_path "host") (or tramp_type "host")]))

(defn insert-file
  ([fullpath]
   (let [[filepath tramp_path tramp_type] (parse-path fullpath)]
     (if-not (is-black? filepath)
       (pg/execute-one! ds
                        ["select add_path(?,?,?)" filepath tramp_path
                         tramp_type]))))
  ([fullpath priority]
   (let [[filepath tramp_path tramp_type] (parse-path fullpath)]
     (if-not (is-black? filepath)
       (pg/execute-one! ds
                        ["select update_priority(?,?,?,?)" filepath tramp_path
                         tramp_type (Integer/parseInt priority)])))))


(if (<= (count *command-line-args*) 2)
  (println (:add_path (apply insert-file *command-line-args*)))
  (println "args are wrong"))




