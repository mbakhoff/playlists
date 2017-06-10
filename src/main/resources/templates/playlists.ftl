<!DOCTYPE html>
<html lang="en">
<head>
  <title>Playlist tracker</title>
  <link rel="stylesheet" href="/assets/bootstrap.css">
  <link rel="stylesheet" href="/assets/playlists.css">
</head>
<body>

<div id="application" class="container">
  <h1>Your playlists</h1>
  <div>Notifications are sent to ${email?html}</div>
  <form action="/logout" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <button class="btn btn-default">Log out</button>
  </form>

  <h2>Add a playlist</h2>
  <form action="/start" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <div class="form-group">
      <label for="url">Playlist URL</label>
      <input type="text" class="form-control" id="url" name="url" placeholder="e.g. https://www.youtube.com/playlist?list=PL7USMo--IcSi5p44jTZTezqS3Z-MEC6DU">
    </div>
    <button class="btn btn-default">Start tracking</button>
  </form>
  <h2>My playlists</h2>
  <form action="/stop" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <div id="playlists">
      <#list playlists as playlist>
        <div class="checkbox">
          <label>
            <#if playlist.title??>
              <input type="checkbox" name="remove" value="${playlist.youtubeId}">
              ${playlist.title?html} (Last synchronized at ${playlist.lastUpdate})
              <a href="https://www.youtube.com/playlist?list=${playlist.youtubeId}"><img src="assets/yt.png" alt="to playlist"/></a>
            <#else>
              <input type="checkbox" name="remove" value="${playlist.youtubeId}">
              ${playlist.youtubeId} (Syncronization pending)
            </#if>
          </label>
        </div>
      </#list>
      <button class="btn btn-default">Stop tracking</button>
    </div>
  </form>

</div>

</body>
</html>
