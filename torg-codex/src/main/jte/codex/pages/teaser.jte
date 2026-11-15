@param de.paladinsinn.torg.codex.web.codex.model.TeaserViewModel model

@template.codex.layout.page(title = model.entryName() + " (Locked)", nav = model.nav(), content = @`
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb small mb-0">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item"><a href="${model.area().canonicalPath()}">${model.area().displayName()}</a></li>
            <li class="breadcrumb-item active" aria-current="page">${model.entryName()}</li>
        </ol>
    </nav>

    <section class="card border-warning shadow-sm my-4">
        <div class="card-header bg-warning-subtle d-flex justify-content-between align-items-center">
            <span class="fw-semibold">
                <i class="bi bi-lock-fill me-2"></i>Purchase Required
            </span>
            <span class="badge text-bg-secondary">${model.area().singularName()}</span>
        </div>
        <div class="card-body p-4">
            <h1 class="h3 mb-2">${model.entryName()}</h1>
            <p class="text-muted mb-4">
                Full rules and statistics for this ${model.area().singularName().toLowerCase()} are available when you own at least one of the following Torg: Eternity publication(s):
            </p>

            <div class="row row-cols-1 row-cols-md-2 g-3 mb-4">
                @for(var prod : model.requiredProducts())
                    <div class="col">
                        <div class="card h-100 border">
                            <div class="card-body d-flex flex-column">
                                <h2 class="h6 card-title mb-1">
                                    @if(prod.publicationPath() != null)
                                        <a href="${prod.publicationPath()}" class="text-decoration-none">${prod.displayName()}</a>
                                    @else
                                        <span>${prod.displayName()}</span>
                                    @endif
                                </h2>
                                <div class="small text-muted font-monospace mb-3">${prod.codexId()}</div>
                                @if(prod.storeUrl() != null)
                                    <div class="mt-auto">
                                        <a href="${prod.storeUrl()}" target="_blank" rel="noopener noreferrer"
                                           class="btn btn-sm btn-primary">
                                            <i class="bi bi-cart3 me-1"></i>Get on DriveThruRPG
                                        </a>
                                    </div>
                                @endif
                            </div>
                        </div>
                    </div>
                @endfor
            </div>

            @if(!model.nav().signedIn())
                <div class="alert alert-light border d-flex flex-wrap justify-content-between align-items-center gap-2 mb-0">
                    <span>Already own one of these publications on DriveThruRPG?</span>
                    <a href="/login" class="btn btn-sm btn-warning">
                        <i class="bi bi-key-fill me-1"></i>Sign In to Unlock
                    </a>
                </div>
            @endif
        </div>
    </section>
`)
