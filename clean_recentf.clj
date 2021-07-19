(require '[babashka.pods :as pods])
(pods/load-pod 'org.babashka/postgresql "0.0.7")

(require '[pod.babashka.postgresql :as pg])

(load-file "/home/weiss/clojure/recentf-db/common.clj")

(def to-check-files
  (case *command-line-args*
    '("all")
    (map :file/filepath
         (pg/execute! ds ["SELECT filepath FROM file WHERE priority >= 3"]))
    (map
      :file/filepath
      (pg/execute!
        ds
        ["SELECT filepath FROM file WHERE priority >= 0 AND tramp_type = 'host' AND tramp_path = 'host'"]))))

(defn check-files [l] (keep (fn [x] (if-not (.exists (io/file x)) x)) l))

(defn disable-file
  [f]
  (pg/execute-one! ds ["select update_priority(?,?,?,?)" f "host" "host" -1]))


(map disable-file (check-files to-check-files))

;; (check-files '("/home/weiss/clojure/recentf-db/clean_recentf.clj" "/ssh:lamp@scilab-0019.cs.uni-kl.de:/home/lamp/django/B9A3.txt"))

