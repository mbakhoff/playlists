-- DROP TABLE IF EXISTS account_playlists CASCADE;
-- DROP TABLE IF EXISTS playlist_item CASCADE;
-- DROP TABLE IF EXISTS playlist CASCADE;
-- DROP TABLE IF EXISTS account CASCADE;
-- DROP SEQUENCE IF EXISTS hibernate_sequence CASCADE;

CREATE SEQUENCE hibernate_sequence;

CREATE TABLE account (
  id       BIGINT PRIMARY KEY,
  email    VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255)        NOT NULL
);

CREATE TABLE playlist (
  id          BIGINT PRIMARY KEY,
  youtube_id  VARCHAR(64) UNIQUE NOT NULL,
  title       VARCHAR(255),
  last_update TIMESTAMP WITH TIME ZONE
);

CREATE TABLE playlist_item (
  playlist_id BIGINT      NOT NULL REFERENCES playlist (id) ON DELETE CASCADE,
  youtube_id  VARCHAR(32) NOT NULL,
  title       VARCHAR(255)
);

CREATE TABLE account_playlists (
  accounts_id  BIGINT NOT NULL REFERENCES account (id),
  playlists_id BIGINT NOT NULL REFERENCES playlist (id)
);

CREATE INDEX playlist_mtime
  ON playlist (last_update);

ALTER TABLE account_playlists
  ADD CONSTRAINT account_playlists_no_duplicates UNIQUE (accounts_id, playlists_id);

ALTER TABLE playlist_item
  ADD CONSTRAINT playlist_item_no_duplicates UNIQUE (playlist_id, youtube_id);
