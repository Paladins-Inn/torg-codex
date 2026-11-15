@param String title = "Torg Codex"
@param de.paladinsinn.torg.codex.web.codex.model.NavContextViewModel nav
@param gg.jte.Content content

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} - Torg Codex</title>
    <link rel="stylesheet" href="/webjars/bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="/webjars/bootstrap-icons/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="/themes/codex/codex.css">
</head>
<body class="codex-reader-theme">
@template.codex.layout.navbar(nav = nav)
<main class="codex-main container py-4">
    ${content}
</main>
@template.codex.layout.footer()
<script src="/webjars/bootstrap/dist/js/bootstrap.bundle.min.js"></script>
<script src="/webjars/htmx.org/dist/htmx.min.js"></script>
<script>
    function initCodexTooltips(root) {
        if (!window.bootstrap || !window.bootstrap.Tooltip) {
            return;
        }
        (root || document).querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (el) {
            window.bootstrap.Tooltip.getOrCreateInstance(el);
        });
    }
    document.addEventListener('DOMContentLoaded', function () { initCodexTooltips(document); });
    document.body.addEventListener('htmx:afterSwap', function (evt) { initCodexTooltips(evt.detail.elt); });
</script>
</body>
</html>
