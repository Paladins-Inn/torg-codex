@param String error = null

<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Torg Codex</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@1/css/pico.min.css">
</head>
<body>
<main class="container">
    <article>
        <header>
            <strong>DriveThruRPG API Login</strong>
        </header>
        @if(error != null)
            <p style="color: red">${error}</p>
        @endif
        <form action="/login" method="post">
            <label for="apiKey">DriveThruRPG API Key</label>
            <input type="password" id="apiKey" name="apiKey" placeholder="Ihr API-Key" required>
            <button type="submit">Login</button>
        </form>
        <footer>
            Bitte geben Sie Ihren DriveThruRPG API-Key ein, um auf geschützte Inhalte zuzugreifen.
        </footer>
    </article>
</main>
</body>
</html>

