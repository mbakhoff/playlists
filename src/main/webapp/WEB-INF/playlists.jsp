<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Playlist tracker</title>
  <base href="${pageContext.request.contextPath}/"/>
  <link rel="stylesheet" href="assets/bootstrap.css">
  <link rel="stylesheet" href="assets/playlists.css">
</head>
<body>

<div id="application" class="container">
  <h1>Your playlists</h1>
  <div>Notifications are sent to <c:out value="${email}"/></div>
  <form action="logout" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <button class="btn btn-default">Log out</button>
  </form>

  <h2>Add a playlist</h2>
  <form action="start" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <div class="form-group">
      <label for="url">Playlist URL</label>
      <input type="text" class="form-control" id="url" name="url" placeholder="e.g. https://www.youtube.com/playlist?list=PL7USMo--IcSi5p44jTZTezqS3Z-MEC6DU">
    </div>
    <button class="btn btn-default">Start tracking</button>
  </form>
  <h2>My playlists</h2>
  <form action="stop" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <div id="playlists">
      <c:forEach items="${playlists}" var="playlist">
        <div class="checkbox">
          <label>
            <c:if test="${playlist.title != null}">
              <input type="checkbox" name="remove" value="${playlist.youtubeId}">
              <c:out value="${playlist.title}"/> (Last synchronized at ${playlist.lastUpdate})
              <a href="https://www.youtube.com/playlist?list=${playlist.youtubeId}"><img src="assets/yt.png" alt="to playlist"/></a>
            </c:if>
            <c:if test="${playlist.title == null}">
              <input type="checkbox" name="remove" value="${playlist.youtubeId}">
              ${playlist.youtubeId} (Syncronization pending)
            </c:if>
          </label>
        </div>
      </c:forEach>
      <button class="btn btn-default">Stop tracking</button>
    </div>
  </form>

</div>

</body>
</html>
