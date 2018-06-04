-- DROP SEQUENCE IF EXISTS hibernate_sequence CASCADE;
-- DROP TABLE IF EXISTS account CASCADE;
-- DROP TABLE IF EXISTS playlist CASCADE;
-- DROP TABLE IF EXISTS playlist_item CASCADE;
-- DROP TABLE IF EXISTS account_playlists CASCADE;
-- DROP TABLE IF EXISTS playlist_playlist_items CASCADE;

CREATE SEQUENCE hibernate_sequence;

CREATE TABLE IF NOT EXISTS account (
  id       BIGINT PRIMARY KEY,
  email    VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255)        NOT NULL
);

CREATE TABLE IF NOT EXISTS playlist (
  id          BIGINT PRIMARY KEY,
  youtube_id  VARCHAR(255) UNIQUE NOT NULL,
  title       VARCHAR(255),
  last_update TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS playlist_item (
  id         BIGINT PRIMARY KEY,
  youtube_id VARCHAR(255) UNIQUE NOT NULL,
  title      VARCHAR(255)        NOT NULL
);

CREATE TABLE IF NOT EXISTS account_playlists (
  accounts_id  BIGINT NOT NULL REFERENCES account (id),
  playlists_id BIGINT NOT NULL REFERENCES playlist (id)
);

CREATE TABLE IF NOT EXISTS playlist_playlist_items (
  playlist_id       BIGINT NOT NULL REFERENCES playlist (id),
  playlist_items_id BIGINT NOT NULL REFERENCES playlist_item (id)
);

CREATE INDEX playlist_mtime
  ON playlist (last_update);

ALTER TABLE account_playlists
  ADD CONSTRAINT account_playlists_no_duplicates UNIQUE (accounts_id, playlists_id);

ALTER TABLE playlist_playlist_items
  ADD CONSTRAINT playlist_playlist_items_no_duplicates UNIQUE (playlist_id, playlist_items_id);
