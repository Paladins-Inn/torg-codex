@param String title = "Torg Codex Administration"
@param de.paladinsinn.torg.codex.web.notification.CsrfViewModel csrf = null
@param de.paladinsinn.torg.codex.web.notification.ToastRegionViewModel toastRegion = null
@param gg.jte.Content content

!{var effectiveRegion = toastRegion != null ? toastRegion : de.paladinsinn.torg.codex.web.notification.ToastRegionViewModel.empty();}
<!DOCTYPE html>
<html lang="en" data-bs-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    @if(csrf != null)
        <meta name="_csrf" content="${csrf.token()}">
        <meta name="_csrf_header" content="${csrf.headerName()}">
        <meta name="_csrf_parameter" content="${csrf.parameterName()}">
    @endif
    <title>${title}</title>
    <link rel="stylesheet" href="/webjars/bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="/webjars/bootstrap-icons/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="/themes/admin/admin.css">
</head>
<body class="codex-admin-theme">
<header class="navbar navbar-dark codex-admin-navbar px-3">
    <span class="navbar-brand mb-0 h1"><i class="bi bi-sliders me-2"></i>Torg Codex Admin</span>
</header>
<div id="admin-toast-container"
     class="toast-container position-fixed top-0 end-0 p-3 admin-toast-region"
     data-stream-url="${effectiveRegion.streamEndpoint()}"
     data-ack-url="${effectiveRegion.ackEndpoint()}">
    @if(!effectiveRegion.initialToasts().isEmpty())
        @template.admin.fragments.toasts(toasts = effectiveRegion.initialToasts())
    @endif
</div>
<main class="container-fluid py-4">
    ${content}
</main>
<script src="/webjars/bootstrap/dist/js/bootstrap.bundle.min.js"></script>
<script src="/webjars/htmx.org/dist/htmx.min.js"></script>
<script src="/js/csrf.js"></script>
<script src="/js/notifications.js"></script>
</body>
</html>
