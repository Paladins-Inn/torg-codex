@param String title = "Torg Codex"
@param de.paladinsinn.torg.codex.web.codex.model.NavContextViewModel nav
@param de.paladinsinn.torg.codex.web.notification.CsrfViewModel csrf = null
@param de.paladinsinn.torg.codex.web.notification.ToastRegionViewModel toastRegion = null
@param gg.jte.Content content

!{var effectiveCsrf = csrf != null ? csrf : (nav != null ? nav.csrf() : null);}
!{var effectiveRegion = toastRegion != null ? toastRegion : (nav != null && nav.toastRegion() != null ? nav.toastRegion() : de.paladinsinn.torg.codex.web.notification.ToastRegionViewModel.empty());}
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    @if(effectiveCsrf != null)
        <meta name="_csrf" content="${effectiveCsrf.token()}">
        <meta name="_csrf_header" content="${effectiveCsrf.headerName()}">
        <meta name="_csrf_parameter" content="${effectiveCsrf.parameterName()}">
    @endif
    <title>${title} - Torg Codex</title>
    <link rel="stylesheet" href="/webjars/bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="/webjars/bootstrap-icons/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="/themes/codex/codex.css">
</head>
<body class="codex-reader-theme">
@template.codex.layout.navbar(nav = nav)
<div id="codex-toast-container"
     class="toast-container position-fixed top-0 end-0 p-3 codex-toast-region"
     data-stream-url="${effectiveRegion.streamEndpoint()}"
     data-ack-url="${effectiveRegion.ackEndpoint()}">
    @if(!effectiveRegion.initialToasts().isEmpty())
        @template.codex.fragments.toasts(toasts = effectiveRegion.initialToasts())
    @endif
</div>
<main class="codex-main container py-4">
    ${content}
</main>
@template.codex.layout.footer()
<script src="/webjars/bootstrap/dist/js/bootstrap.bundle.min.js"></script>
<script src="/webjars/htmx.org/dist/htmx.min.js"></script>
<script src="/js/csrf.js"></script>
<script src="/js/notifications.js"></script>
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
