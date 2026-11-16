(function () {
    'use strict';

    var DISPLAYED_KEY = 'torg.codex.displayedToasts';
    var PENDING_ACK_KEY = 'torg.codex.pendingToastAcks';
    var MAX_STORED_IDS = 200;
    var ACK_BATCH_LIMIT = 20;
    var ACK_RETRY_DELAY_MS = 1500;

    var activeEventSource = null;
    var unloading = false;
    var retryTimer = null;

    function readIdList(storageKey) {
        try {
            var raw = window.sessionStorage.getItem(storageKey);
            if (!raw) {
                return [];
            }
            var parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed : [];
        } catch (ignored) {
            return [];
        }
    }

    function writeIdList(storageKey, ids) {
        try {
            var trimmed = ids.length > MAX_STORED_IDS ? ids.slice(ids.length - MAX_STORED_IDS) : ids;
            window.sessionStorage.setItem(storageKey, JSON.stringify(trimmed));
        } catch (ignored) {
            // Storage unavailable or full.
        }
    }

    function hasDisplayed(id) {
        return readIdList(DISPLAYED_KEY).indexOf(id) !== -1;
    }

    function markDisplayed(id) {
        var list = readIdList(DISPLAYED_KEY);
        if (list.indexOf(id) === -1) {
            list.push(id);
            writeIdList(DISPLAYED_KEY, list);
        }
    }

    function enqueuePendingAck(id) {
        var list = readIdList(PENDING_ACK_KEY);
        if (list.indexOf(id) === -1) {
            list.push(id);
            writeIdList(PENDING_ACK_KEY, list);
        }
    }

    function removePendingAcks(ackedIds) {
        var remaining = readIdList(PENDING_ACK_KEY).filter(function (item) {
            return ackedIds.indexOf(item) === -1;
        });
        writeIdList(PENDING_ACK_KEY, remaining);
    }

    function scheduleAckRetry(ackUrl) {
        if (retryTimer || unloading || !ackUrl) {
            return;
        }
        retryTimer = window.setTimeout(function () {
            retryTimer = null;
            flushPendingAcks(ackUrl);
        }, ACK_RETRY_DELAY_MS);
    }

    function flushPendingAcks(ackUrl) {
        if (!ackUrl || unloading) {
            return;
        }
        if (window.TorgCsrf && !window.TorgCsrf.isSameOrigin(ackUrl)) {
            return;
        }
        var pending = readIdList(PENDING_ACK_KEY);
        if (!pending.length) {
            return;
        }
        var csrf = window.TorgCsrf ? window.TorgCsrf.readToken() : null;
        if (!csrf) {
            return;
        }
        var batch = pending.slice(0, ACK_BATCH_LIMIT);
        var params = new URLSearchParams();
        batch.forEach(function (id) {
            params.append('id', id);
        });
        var headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        headers[csrf.headerName] = csrf.token;

        window.fetch(ackUrl, {
            method: 'POST',
            credentials: 'same-origin',
            headers: headers,
            body: params.toString()
        }).then(function (response) {
            if (response.status === 204 || response.status === 200) {
                removePendingAcks(batch);
                if (readIdList(PENDING_ACK_KEY).length > 0) {
                    flushPendingAcks(ackUrl);
                }
            } else if (response.status >= 500) {
                scheduleAckRetry(ackUrl);
            }
        }).catch(function () {
            scheduleAckRetry(ackUrl);
        });
    }

    function activateToastElement(el, ackUrl) {
        var notificationId = el.getAttribute('data-notification-id');
        if (notificationId && hasDisplayed(notificationId)) {
            enqueuePendingAck(notificationId);
            if (el.parentNode) {
                el.parentNode.removeChild(el);
            }
            flushPendingAcks(ackUrl);
            return;
        }

        el.setAttribute('data-toast-init', 'ready');
        el.addEventListener('hidden.bs.toast', function () {
            if (window.bootstrap && window.bootstrap.Toast) {
                var existing = window.bootstrap.Toast.getInstance(el);
                if (existing) {
                    existing.dispose();
                }
            }
            if (el.parentNode) {
                el.parentNode.removeChild(el);
            }
        });

        var shown = false;
        if (window.bootstrap && window.bootstrap.Toast) {
            var autoHide = el.getAttribute('data-bs-autohide') === 'true';
            var delayMs = parseInt(el.getAttribute('data-bs-delay') || '0', 10);
            var instance = window.bootstrap.Toast.getOrCreateInstance(el, {
                autohide: autoHide,
                delay: autoHide && delayMs > 0 ? delayMs : 6000
            });
            instance.show();
            shown = true;
        } else {
            el.classList.add('show');
            shown = true;
        }

        if (shown && notificationId && ackUrl) {
            markDisplayed(notificationId);
            enqueuePendingAck(notificationId);
            flushPendingAcks(ackUrl);
        }
    }

    function initContainerToasts(container) {
        if (!container) {
            return;
        }
        var ackUrl = container.getAttribute('data-ack-url');
        var pendingEls = Array.prototype.slice.call(
            container.querySelectorAll('.toast[data-toast-init="pending"]'));
        pendingEls.forEach(function (el) {
            activateToastElement(el, ackUrl);
        });
        if (ackUrl) {
            flushPendingAcks(ackUrl);
        }
    }

    function appendToastFragment(container, html) {
        if (!container || !html) {
            return;
        }
        var template = document.createElement('template');
        template.innerHTML = html;
        var nodes = Array.prototype.slice.call(template.content.querySelectorAll('.toast'));
        var ackUrl = container.getAttribute('data-ack-url');
        nodes.forEach(function (toastNode) {
            var id = toastNode.getAttribute('data-notification-id');
            if (id && (hasDisplayed(id) || container.querySelector('[data-notification-id="' + id + '"]'))) {
                enqueuePendingAck(id);
                flushPendingAcks(ackUrl);
                return;
            }
            container.appendChild(toastNode);
            activateToastElement(toastNode, ackUrl);
        });
    }

    function connectStream(container) {
        if (!container || typeof window.EventSource !== 'function') {
            return;
        }
        var streamUrl = container.getAttribute('data-stream-url');
        if (!streamUrl) {
            return;
        }
        if (window.TorgCsrf && !window.TorgCsrf.isSameOrigin(streamUrl)) {
            return;
        }
        closeStream();
        var source = new window.EventSource(streamUrl, { withCredentials: true });
        activeEventSource = source;

        source.addEventListener('toast', function (evt) {
            if (evt && evt.data) {
                appendToastFragment(container, evt.data);
            }
        });

        source.onerror = function () {
            if (unloading || source.readyState === window.EventSource.CLOSED) {
                closeStream();
            }
        };
    }

    function closeStream() {
        if (activeEventSource) {
            try {
                activeEventSource.close();
            } catch (ignored) {
                // Already closed.
            }
            activeEventSource = null;
        }
    }

    function findToastContainer() {
        return document.getElementById('codex-toast-container')
            || document.getElementById('admin-toast-container');
    }

    document.addEventListener('click', function (evt) {
        var trigger = evt.target && evt.target.closest
            ? evt.target.closest('[data-toast-action="reload"]')
            : null;
        if (trigger) {
            evt.preventDefault();
            window.location.reload();
        }
    });

    window.addEventListener('pagehide', function () {
        unloading = true;
        closeStream();
    });
    window.addEventListener('beforeunload', function () {
        unloading = true;
        closeStream();
    });

    document.addEventListener('DOMContentLoaded', function () {
        var container = findToastContainer();
        if (container) {
            initContainerToasts(container);
            connectStream(container);
        }
    });

    document.body.addEventListener('htmx:afterSwap', function (evt) {
        var container = findToastContainer();
        if (container && evt && evt.detail && evt.detail.target === container) {
            initContainerToasts(container);
        }
    });
})();
