(require '[babashka.pods :as pods])
(pods/load-pod 'org.babashka/postgresql "0.0.7")

(require '[pod.babashka.postgresql :as pg])

(load-file "/home/weiss/clojure/recentf-db/common.clj")

;; (def *command-line-args* '("nil" "txt" ",all"))

(if (clojure.string/includes? (first *command-line-args*) ":")
  (let [[type path] (clojure.string/split (first *command-line-args*) #":")]
    (def project-root "nil")
    (def tramp-type type)
    (def tramp-path path))
  (do (def project-root (first *command-line-args*)) (def tramp-type nil) (def tramp-path nil)))
;; (println tramp-type tramp-path)
(if (clojure.string/starts-with? (last *command-line-args*) ",")
  (do (def cmd-args (drop-last 1 (drop 1 *command-line-args*)))
      (def extra-info (subs (last *command-line-args*) 1)))
  (do (def cmd-args (drop 1 *command-line-args*)) (def extra-info nil)))

(def sep "ǁ    ")
(def sep-for-tree "』𝔰𝔢𝔭『")

(let
  [temp
   {:host
    "SELECT filepath as filepath, freq / (extract(day from age(current_timestamp, last_ref)) + 0.5) as weight FROM file WHERE priority=0 AND tramp_type = 'host' AND tramp_path = 'host' AND ",
    :bookmark
    "SELECT filepath as filepath, freq / (extract(day from age(current_timestamp, last_ref)) + 0.5) as weight FROM file WHERE priority=1 AND "}]
  (def query-map
    (cond
      (and tramp-type tramp-path)
      (conj
       temp
       {:tramp
        (format
         "SELECT filepath as filepath, freq / (extract(day from age(current_timestamp, last_ref)) + 0.5) as weight FROM file WHERE tramp_path='%s' AND tramp_type='%s' AND "
         tramp-path
         tramp-type)})
      extra-info
      (case extra-info
        "all"
        (conj
         temp
         {:tramp
          "SELECT filepath as filepath, tramp_path as tramp_path, tramp_type as tramp_type, freq / (extract(day from age(current_timestamp, last_ref)) + 0.5) as weight FROM file WHERE tramp_path != 'host' AND tramp_type !='host' AND "})
        (conj
         temp
         {:tramp
          (str
           "SELECT filepath as filepath, freq / (extract(day from age(current_timestamp, last_ref)) + 0.5) as weight FROM file WHERE tramp_path != 'host' AND tramp_type !='host' AND tramp_path like  '%"
           extra-info
           "'% AND ")}))
      :else temp)))

(def type-icon {:bookmark " ", :tramp " "})

;; comes from
;; https://stackoverflow.com/questions/49515858/clojure-convert-file-path-to-tree
(defn as-tree [data] (map (fn [[k vs]] (cons k (as-tree (keep next vs)))) (group-by first data)))

(defn path->tree
  [path-list]
  (as-tree (map (comp #(clojure.string/split % #"/") #(subs % 1)) path-list)))

(defn update-last-path-value
  ([l suffix] (update-last-path-value l suffix ""))
  ([l suffix prefix] (let [end (last l)] (conj (pop l) (str prefix end suffix)))))

(defn zip-tree
  ([l] (zip-tree l [""]))
  ([l res]
   (let [path     (first l)
         children (drop 1 l)
         c        (count children)]
     (case c
       0 (update-last-path-value res path)
       1 (zip-tree (first children) (update-last-path-value res (str path "/")))
       (let [new-res (update-last-path-value res (str path "/"))]
         (into new-res
               (map #(zip-tree % [(str (re-find (re-pattern (str "^.*" sep)) (last new-res)) sep)])
                    children)))))))

(defn get-file
  ([type]
   (let [res (pg/execute! ds
                          [(str (type query-map)
                                (reduce (fn [l e] (str l " AND " e))
                                        (map (fn [x]
                                               (str "LOWER(filepath) like '%"
                                                    (clojure.string/lower-case x)
                                                    "%'"))
                                             cmd-args)))])]
     ;; res
     (if (= type :tramp)
       (map (fn [x]
              {:file/filepath (str (:file/tramp_type x)
                                   ":" (:file/tramp_path x)
                                   ":" (:file/filepath x)),
               :weight        (:weight x)})
            res)
       res))))


(def get-file-memoize (memoize get-file))

(defn get-weight
  [type prefix]
  (->> (keep (fn [l] (when (clojure.string/starts-with? (:file/filepath l) prefix) (:weight l)))
             (get-file-memoize type))
       (#(/ (reduce + %) (count %)))
       ((fn [x] (if (clojure.string/starts-with? prefix project-root) (+ 99999 x) x)))))


(def get-weight-memoize (memoize get-weight))

(defn sort-tree
  ([type tree] (sort-tree type tree "/"))
  ([type tree prefix]
   (if (and (vector? tree) (> (count tree) 1))
     ;; (let [filter-sep (fn [x] (re-find #"[a-zA-z.0-9].*$" x))
     (let [filter-sep (fn [x] (clojure.string/replace x (re-pattern (str sep "*")) ""))
           new-prefix (str prefix (filter-sep (first tree)))]
       (conj (sort-by (fn [x] (get-weight-memoize type (str new-prefix (filter-sep (first x)))))
                      #(compare %2 %1)
                      (map #(sort-tree type % (str new-prefix)) (drop 1 tree)))
             (first tree)))
     tree)))

(defn expand-tree
  ([tree] (expand-tree tree "/"))
  ([tree prefix]
   (let [new-prefix (str prefix (clojure.string/replace (first tree) (re-pattern sep) ""))]
     (cons new-prefix
           (map (fn [x]
                  (cons (str new-prefix (clojure.string/replace (first x) (re-pattern sep) ""))
                        (drop 1 (expand-tree x new-prefix))))
                (drop 1 tree))))))

(defn merge-tree [trees] (if-let [[x y] trees] (map (fn [x y] (str x sep-for-tree y)) x y)))

(defn add-icon
  [type l]
  (if-let [icon (type type-icon)]
    (map #(str (type type-icon) %) l)
    l))

(defn search-with-type
  [type]
  (some->> type
           get-file-memoize
           (map (comp first vals))
           (path->tree)
           (first)
           (zip-tree)
           (sort-tree type)
           ((fn [x] [x (expand-tree x)]))
           (map flatten)
           ((fn [xs] (cons (add-icon type (first xs)) (rest xs))))
           (merge-tree)
           (clojure.string/join \newline)))

(defn print-all
  [types]
  (some->> (keep search-with-type types)
           not-empty
           (clojure.string/join \newline)
           (println)))

;; (println "\033[0;31mFoo\033[0m")
(if (contains? query-map :tramp) (print-all [:bookmark :tramp :host]) (print-all [:bookmark :host]))


