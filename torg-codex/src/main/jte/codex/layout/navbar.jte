@param de.paladinsinn.torg.codex.web.codex.model.NavContextViewModel nav

<nav class="navbar navbar-expand-lg navbar-dark codex-navbar shadow-sm" aria-label="Main navigation">
    <div class="container">
        <a class="navbar-brand fw-bold d-flex align-items-center" href="/">
            <i class="bi bi-journal-richtext me-2 text-warning"></i>
            <span>Torg Codex</span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#codexNavbarContent" aria-controls="codexNavbarContent"
                aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="codexNavbarContent">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                @for(var group : nav.sections())
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle${nav.isSectionActive(group.section()) ? " active" : ""}"
                           href="#" id="nav-${group.section().getSlug()}" role="button"
                           data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="bi ${group.section().getIcon()} me-1"></i>${group.section().getDisplayName()}
                        </a>
                        <ul class="dropdown-menu" aria-labelledby="nav-${group.section().getSlug()}">
                            @for(var area : group.areas())
                                <li>
                                    <a class="dropdown-item${nav.activeArea() == area ? " active" : ""}"
                                       href="${area.canonicalPath()}">
                                        <i class="bi ${area.icon()} me-2"></i>${area.displayName()}
                                    </a>
                                </li>
                            @endfor
                        </ul>
                    </li>
                @endfor
            </ul>

            <div class="d-flex align-items-center gap-2">
                <span class="badge rounded-pill text-bg-secondary">
                    <i class="bi ${nav.signedIn() ? "bi-unlock-fill" : "bi-book"} me-1"></i>${nav.authStatusTag()}
                </span>
                @if(nav.signedIn())
                    <a class="btn btn-sm btn-outline-light" href="/logout">
                        <i class="bi bi-box-arrow-right me-1"></i>Sign Out
                    </a>
                @else
                    <a class="btn btn-sm btn-warning" href="/login">
                        <i class="bi bi-key-fill me-1"></i>Sign In
                    </a>
                @endif
            </div>
        </div>
    </div>
</nav>
