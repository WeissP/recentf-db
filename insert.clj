(require '[babashka.pods :as pods])
(pods/load-pod 'org.babashka/postgresql "0.0.7")

(require '[pod.babashka.postgresql :as pg])

(load-file "/home/weiss/clojure/recentf-db/common.clj")

;; the key is the path, the value is the re for the filename
(def black-list
  {"/home/weiss/.emacs.d/emacs-config" #".+<.+\.el",
   "/home/weiss/Dropbox/Org-roam" #"^Ʀ.+\.org"})

(defn split-first [re s] (clojure.string/split s re 2))

(defn split-last
  [re s]
  (let [pattern (re-pattern (str re "(?!.*" re ")"))] (split-first pattern s)))

(defn is-black?
  ([full-filepath]
   (let [[prefix last] (split-last #"/" full-filepath)]
     (is-black? prefix last)))
  ([filepath filename]
   (let [[prefix last] (split-last #"/" filepath)]
     (or (some->> filepath
                  (get black-list)
                  (#(re-matches % filename)))
         (and last (is-black? prefix filename))))))

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




