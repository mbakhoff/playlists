<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Login</title>
  <base href="${pageContext.request.contextPath}/"/>
  <link rel="stylesheet" href="assets/bootstrap.css">
  <link rel="stylesheet" href="assets/login.css">
</head>
<body>

<div id="application" class="container">
  <h1>Youtube playlist tracker</h1>
  <form method="post">
    <c:if test="${param.error != null}">
      <p>
        Invalid username and password.
      </p>
    </c:if>
    <c:if test="${param.logout != null}">
      <p>
        You have been logged out.
      </p>
    </c:if>
    <c:if test="${signup != null}">
      <p>
          ${signup}
      </p>
    </c:if>
    <p>
      <label for="username">E-mail</label>
      <input type="text" id="username" name="username"/>
    </p>
    <p>
      <label for="password">Password</label>
      <input type="password" id="password" name="password"/>
    </p>
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <button type="submit" formaction="auth/login" class="btn btn-primary">Log in</button>
    <button type="submit" formaction="auth/signup" class="btn">Sign up</button>
  </form>
</div>

</body>
</html>
