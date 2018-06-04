ALTER SEQUENCE hibernate_sequence RESTART WITH 100;

INSERT INTO account (id, email, password) VALUES (1, 'email1', 'pass1');
INSERT INTO account (id, email, password) VALUES (2, 'email2', 'pass2');

INSERT INTO playlist (id, youtube_id, title, last_update) VALUES (3, 'yid3', 'title3', '2017-01-03T00:00:00Z');
INSERT INTO playlist (id, youtube_id, title, last_update) VALUES (4, 'yid4', 'title4', '2017-01-04T00:00:00Z');
INSERT INTO playlist (id, youtube_id, title, last_update) VALUES (5, 'yid5', 'title5', NULL);

INSERT INTO playlist_item (id, youtube_id, title) VALUES (6, 'yid6', 'title6');
INSERT INTO playlist_item (id, youtube_id, title) VALUES (7, 'yid7', 'title7');
INSERT INTO playlist_item (id, youtube_id, title) VALUES (8, 'yid8', 'title8');

INSERT INTO account_playlists (accounts_id, playlists_id) VALUES (1, 3);
INSERT INTO account_playlists (accounts_id, playlists_id) VALUES (1, 4);
INSERT INTO account_playlists (accounts_id, playlists_id) VALUES (2, 4);
INSERT INTO account_playlists (accounts_id, playlists_id) VALUES (2, 5);

INSERT INTO playlist_playlist_items (playlist_id, playlist_items_id) VALUES (3, 6);
INSERT INTO playlist_playlist_items (playlist_id, playlist_items_id) VALUES (3, 7);
INSERT INTO playlist_playlist_items (playlist_id, playlist_items_id) VALUES (4, 7);
INSERT INTO playlist_playlist_items (playlist_id, playlist_items_id) VALUES (4, 8);
INSERT INTO playlist_playlist_items (playlist_id, playlist_items_id) VALUES (5, 6);
INSERT INTO playlist_playlist_items (playlist_id, playlist_items_id) VALUES (5, 7);
INSERT INTO playlist_playlist_items (playlist_id, playlist_items_id) VALUES (5, 8);
