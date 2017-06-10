<!DOCTYPE html>
<html lang="en">
<head>
  <title>Login</title>
  <link rel="stylesheet" href="/assets/bootstrap.css">
  <link rel="stylesheet" href="/assets/login.css">
</head>
<body>

<div id="application" class="container">
  <h1>Youtube playlist tracker</h1>
  <form method="post">
    <#if RequestParameters.error??>
      <p>
        Invalid username and password.
      </p>
    </#if>
    <#if RequestParameters.logout??>
      <p>
        You have been logged out.
      </p>
    </#if>
    <p>
      <label for="username">E-mail</label>
      <input type="text" id="username" name="username"/>
    </p>
    <p>
      <label for="password">Password</label>
      <input type="password" id="password" name="password"/>
    </p>
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <button type="submit" formaction="/auth/login" class="btn btn-primary">Log in</button>
    <button type="submit" formaction="/auth/signup" class="btn">Sign up</button>
  </form>
</div>

</body>
</html>
