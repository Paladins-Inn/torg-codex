@param String title = "Torg Codex Administration"
@param gg.jte.Content content

<!DOCTYPE html>
<html lang="en" data-bs-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <link rel="stylesheet" href="/webjars/bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="/webjars/bootstrap-icons/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="/themes/admin/admin.css">
</head>
<body class="codex-admin-theme">
<header class="navbar navbar-dark codex-admin-navbar px-3">
    <span class="navbar-brand mb-0 h1"><i class="bi bi-sliders me-2"></i>Torg Codex Admin</span>
</header>
<main class="container-fluid py-4">
    ${content}
</main>
<script src="/webjars/bootstrap/dist/js/bootstrap.bundle.min.js"></script>
<script src="/webjars/htmx.org/dist/htmx.min.js"></script>
</body>
</html>
