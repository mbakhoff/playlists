DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS playlist CASCADE;
DROP TABLE IF EXISTS playlist_items CASCADE;
DROP TABLE IF EXISTS account_playlist CASCADE;

CREATE TABLE IF NOT EXISTS account (
  id       BIGSERIAL PRIMARY KEY,
  email    VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255)        NOT NULL
);

CREATE TABLE IF NOT EXISTS playlist (
  id         BIGSERIAL PRIMARY KEY,
  youtubeId  VARCHAR(255) UNIQUE NOT NULL,
  title      VARCHAR(255),
  lastUpdate TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS playlist_items (
  id          BIGSERIAL PRIMARY KEY,
  playlist_id BIGINT       NOT NULL REFERENCES playlist (id) ON DELETE CASCADE,
  youtubeId   VARCHAR(255) NOT NULL,
  title       VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS account_playlist (
  account_id   BIGINT NOT NULL REFERENCES account (id) ON DELETE CASCADE,
  playlists_id BIGINT NOT NULL REFERENCES playlist (id)
);

CREATE INDEX playlist_mtime
  ON playlist (lastUpdate);

ALTER TABLE account_playlist
  ADD CONSTRAINT account_playlist_no_duplicates UNIQUE (account_id, playlists_id);

ALTER TABLE playlist_items
  ADD CONSTRAINT playlist_items_no_duplicates UNIQUE (playlist_id, youtubeId);
