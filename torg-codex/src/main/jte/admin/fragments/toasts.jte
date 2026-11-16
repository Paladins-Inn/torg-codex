@param java.util.List<de.paladinsinn.torg.codex.web.notification.ToastViewModel> toasts

@for(var toast : toasts)
    <div id="${toast.content().id()}"
         class="toast admin-toast admin-toast-${toast.presentation().severityKey()} text-bg-dark border-secondary shadow mb-2"
         data-notification-id="${toast.content().notificationId()}"
         data-toast-init="pending"
         role="${toast.presentation().ariaRole()}"
         aria-live="${toast.presentation().ariaLive()}"
         aria-atomic="true"
         data-bs-autohide="${String.valueOf(toast.presentation().autoHide())}"
         data-bs-delay="${toast.presentation().delayMs()}">
        <div class="toast-header bg-dark text-light border-secondary">
            <i class="bi ${toast.presentation().iconCss()} me-2"></i>
            <strong class="me-auto">${toast.content().title()}</strong>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
        <div class="toast-body">
            <div>${toast.content().message()}</div>
            @if(toast.action() != null)
                <div class="mt-2 pt-2 border-top border-secondary">
                    @if("reload".equals(toast.action().kind()))
                        <button type="button"
                                class="btn btn-sm btn-info"
                                data-toast-action="reload">
                            <i class="bi bi-arrow-clockwise me-1"></i>${toast.action().label()}
                        </button>
                    @elseif(toast.action().targetPath() != null)
                        <a href="${toast.action().targetPath()}"
                           class="btn btn-sm btn-outline-info">
                            ${toast.action().label()}
                        </a>
                    @endif
                </div>
            @endif
        </div>
    </div>
@endfor
