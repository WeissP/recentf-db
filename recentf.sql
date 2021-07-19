DROP TABLE IF EXISTS file;

CREATE TABLE file 
  (
    tramp_type varchar,
    tramp_path varchar,
    filepath varchar not null,
    last_ref date,
    freq int,
    priority int,
    PRIMARY KEY (tramp_type, tramp_path, filepath)
  );

CREATE OR REPLACE FUNCTION add_path (this_filepath varchar, this_tramp_path varchar,this_tramp_type varchar)
  RETURNS varchar  AS $$
  DECLARE res varchar;
    BEGIN
      IF exists(SELECT * 
                  FROM file 
                 WHERE filepath = this_filepath
                   AND tramp_path = this_tramp_path
                   AND tramp_type = this_tramp_type)
          THEN
              UPDATE file
              SET last_ref = current_timestamp, freq = freq + 1
              WHERE filepath = this_filepath
              AND tramp_path = this_tramp_path
              AND tramp_type = this_tramp_type;
              SELECT 'updated' INTO res;
              ELSE
                  INSERT INTO file Values(this_tramp_type, this_tramp_path, this_filepath, current_timestamp, 1, 0) ;
                  SELECT 'added' INTO res;
          END IF;
      RETURN res;
    END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_priority (this_filepath varchar, this_tramp_path varchar,this_tramp_type varchar, this_priority bigint)
  RETURNS varchar AS $$
  DECLARE res varchar;
    BEGIN
      IF exists(SELECT * 
                  FROM file 
                 WHERE filepath = this_filepath
                   AND tramp_path = this_tramp_path
                   AND tramp_type = this_tramp_type)
          THEN
              UPDATE file
              SET priority = this_priority
              WHERE filepath = this_filepath
              AND tramp_path = this_tramp_path
              AND tramp_type = this_tramp_type;
              SELECT 'priority updated' INTO res;
              ELSE
                  INSERT INTO file Values(this_tramp_type, this_tramp_path, this_filepath, current_timestamp, 1, this_priority) ;
                  SELECT 'file added and priority updated' INTO res;
          END IF;
      RETURN res;
    END; $$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE disable-file (this_filepath varchar, this_tramp_path varchar,this_tramp_type varchar)
  LANGUAGE plpgsql;
    AS $$
      DECLARE
      BEGIN
        UPDATE file
           SET priority = 0
         WHERE filepath = this_filepath
           AND tramp_path = this_tramp_path
           AND tramp_type = this_tramp_type;
        RETURN;
      END; $$


SELECT *
FROM file
WHERE filepath like 'fds%';


SELECT filepath FROM file WHERE filepath like '%' ORDER BY freq desc;

select exists(SELECT * FROM file WHERE filepath = '/home/weiss/clojure/recentf-db/insert.clj'  AND tramp_path = 'host'  AND tramp_type = 'host'  AND tag = 'bookmark');
