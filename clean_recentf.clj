(require '[babashka.pods :as pods])
(pods/load-pod 'org.babashka/postgresql "0.0.7")

(require '[pod.babashka.postgresql :as pg])

(load-file "/home/weiss/clojure/recentf-db/common.clj")
(load-file "/home/weiss/clojure/recentf-db/black_list.clj")

;; (def *command-line-args* '("restore"))

(def to-check-files
  (case (or (first *command-line-args*) "default")
    "restore"
      (map :file/filepath
        (pg/execute!
          ds
          ["SELECT filepath FROM file WHERE priority < 0 AND tramp_type =
          'host' AND tramp_path = 'host'"]))
    ("default" "blacklist")
      (map :file/filepath
        (pg/execute!
          ds
          ["SELECT filepath FROM file WHERE priority >= 0 AND tramp_type = 'host' AND tramp_path = 'host'"]))))

(defn exists? [f] (.exists (io/file f)))

(defn filter-files
  [get-exists l]
  (keep (fn [x] (if (= get-exists (exists? x)) x)) l))

(defn disable-file
  [f]
  (pg/execute-one! ds ["select update_priority(?,?,?,?)" f "host" "host" -1])
  f)

(defn restore-file
  [f]
  (pg/execute-one! ds ["select update_priority(?,?,?,?)" f "host" "host" 0])
  f)

(defn restore-files [] (map restore-file (filter-files true to-check-files)))

(defn disable-files [] (map disable-file (filter-files false to-check-files)))

(defn processing-blacklist
  []
  (map disable-file
    (keep (fn [x] (if (is-black? x) x)) (filter-files true to-check-files))))

(case (or (first *command-line-args*) "default")
  "restore" (restore-files)
  "blacklist" (processing-blacklist)
  "default" (disable-files))



