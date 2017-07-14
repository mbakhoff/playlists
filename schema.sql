DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS playlist CASCADE;
DROP TABLE IF EXISTS playlist_items CASCADE;
DROP TABLE IF EXISTS account_playlists CASCADE;

CREATE SEQUENCE hibernate_sequence;

CREATE TABLE IF NOT EXISTS account (
  id       BIGSERIAL PRIMARY KEY,
  email    VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255)        NOT NULL
);

CREATE TABLE IF NOT EXISTS playlist (
  id          BIGSERIAL PRIMARY KEY,
  youtube_id  VARCHAR(255) UNIQUE NOT NULL,
  title       VARCHAR(255),
  last_update TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS playlist_item (
  id          BIGSERIAL PRIMARY KEY,
  youtube_id  VARCHAR(255) NOT NULL,
  title       VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS account_playlists (
  accounts_id  BIGINT NOT NULL REFERENCES account (id) ON DELETE CASCADE,
  playlists_id BIGINT NOT NULL REFERENCES playlist (id)
);

CREATE TABLE IF NOT EXISTS playlist_playlist_items (
  playlist_id  BIGINT NOT NULL REFERENCES playlist (id) ON DELETE CASCADE,
  playlist_items_id BIGINT NOT NULL REFERENCES playlist_item (id)
);

CREATE INDEX playlist_mtime
  ON playlist (lastUpdate);

ALTER TABLE account_playlists
  ADD CONSTRAINT account_playlist_no_duplicates UNIQUE (accounts_id, playlists_id);

ALTER TABLE playlist_playlist_items
  ADD CONSTRAINT playlist_playlist_items_no_duplicates UNIQUE (playlist_id, playlist_items_id);

ALTER TABLE playlist_items
  ADD CONSTRAINT playlist_items_no_duplicates UNIQUE (playlist_id, youtubeId);
