DROP SEQUENCE IF EXISTS hibernate_sequence;
DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS playlist CASCADE;
DROP TABLE IF EXISTS playlist_items CASCADE;
DROP TABLE IF EXISTS account_playlist CASCADE;

CREATE SEQUENCE hibernate_sequence;

CREATE TABLE IF NOT EXISTS account (
  id       BIGSERIAL PRIMARY KEY,
  version  BIGINT DEFAULT 0,
  email    VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255)        NOT NULL
);

CREATE TABLE IF NOT EXISTS playlist (
  id         BIGSERIAL PRIMARY KEY,
  version    BIGINT DEFAULT 0,
  youtubeId  VARCHAR(255) UNIQUE NOT NULL,
  title      VARCHAR(255),
  lastUpdate TIMESTAMP
);

CREATE TABLE IF NOT EXISTS playlist_items (
  playlist_id BIGINT       NOT NULL REFERENCES playlist (id),
  youtubeId   VARCHAR(255) NOT NULL,
  title       VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS account_playlist (
  account_id   BIGINT NOT NULL REFERENCES account (id),
  playlists_id BIGINT NOT NULL REFERENCES playlist (id)
);

CREATE INDEX playlist_mtime ON playlist (lastUpdate);

ALTER TABLE account_playlist
  ADD CONSTRAINT no_duplicates UNIQUE (account_id, playlists_id);
