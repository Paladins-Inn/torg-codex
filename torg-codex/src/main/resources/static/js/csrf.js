(function () {
    'use strict';

    var SAFE_METHODS = { GET: true, HEAD: true, OPTIONS: true, TRACE: true };

    function readMetaContent(name) {
        var el = document.querySelector('meta[name="' + name + '"]');
        return el ? el.getAttribute('content') : null;
    }

    function readToken() {
        var token = readMetaContent('_csrf');
        var headerName = readMetaContent('_csrf_header') || 'X-CSRF-TOKEN';
        var parameterName = readMetaContent('_csrf_parameter') || '_csrf';
        if (!token) {
            return null;
        }
        return {
            token: token,
            headerName: headerName,
            parameterName: parameterName
        };
    }

    function isSameOrigin(targetUrl) {
        if (!targetUrl) {
            return true;
        }
        try {
            var resolved = new URL(String(targetUrl), window.location.origin);
            return resolved.origin === window.location.origin;
        } catch (ignored) {
            return false;
        }
    }

    document.addEventListener('htmx:configRequest', function (event) {
        var detail = event && event.detail;
        if (!detail || !detail.headers) {
            return;
        }
        var verb = String(detail.verb || 'GET').toUpperCase();
        if (SAFE_METHODS[verb]) {
            return;
        }
        if (!isSameOrigin(detail.path)) {
            return;
        }
        var csrf = readToken();
        if (csrf) {
            detail.headers[csrf.headerName] = csrf.token;
        }
    });

    window.TorgCsrf = {
        readToken: readToken,
        isSameOrigin: isSameOrigin
    };
})();
