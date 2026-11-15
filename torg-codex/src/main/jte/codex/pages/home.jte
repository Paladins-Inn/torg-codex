@param de.paladinsinn.torg.codex.web.codex.model.HomeViewModel model

@template.codex.layout.page(title = "Home", nav = model.nav(), content = @`
    <section class="card border-0 shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
                <div>
                    <h1 class="h3 mb-1">Torg: Eternity Reference Codex</h1>
                    @if(model.nav().signedIn())
                        <p class="text-muted mb-0">
                            Signed in via DriveThruRPG — <strong>${model.ownedPublications().size()}</strong> unlocked publication(s) active.
                        </p>
                    @else
                        <p class="text-muted mb-0">
                            You are browsing free-tier <strong>Core Rulebook</strong> content.
                            <a href="/login">Sign in with your DriveThruRPG key</a> to unlock purchased sourcebooks.
                        </p>
                    @endif
                </div>
                <div>
                    @if(model.nav().signedIn())
                        <a href="/logout" class="btn btn-outline-secondary btn-sm">
                            <i class="bi bi-box-arrow-right me-1"></i>Sign Out
                        </a>
                    @else
                        <a href="/login" class="btn btn-primary btn-sm">
                            <i class="bi bi-key-fill me-1"></i>Unlock My Books
                        </a>
                    @endif
                </div>
            </div>
            <div class="progress" role="progressbar" aria-label="Library unlock progress"
                 aria-valuenow="${model.unlockedPercent()}" aria-valuemin="0" aria-valuemax="100"
                 style="height: 0.75rem;">
                <div class="progress-bar bg-warning" style="width: ${model.unlockedPercent()}%"></div>
            </div>
        </div>
    </section>

    <section class="mb-5" aria-labelledby="categories-heading">
        <h2 id="categories-heading" class="h4 mb-3">
            <i class="bi bi-grid-3x3-gap me-2"></i>Codex Categories
        </h2>
        <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-xl-4 g-3">
            @for(var card : model.categories())
                <div class="col">
                    <div class="card h-100 shadow-sm position-relative">
                        <div class="card-body d-flex flex-column">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <h3 class="h6 card-title mb-0">
                                    <i class="bi ${card.area().icon()} me-2 text-primary"></i>${card.area().displayName()}
                                </h3>
                                <span class="badge rounded-pill text-bg-light border">${card.accessibleCount()}</span>
                            </div>
                            <p class="card-text small text-muted mb-0 flex-grow-1">${card.area().description()}</p>
                            <a href="${card.area().canonicalPath()}" class="stretched-link"
                               aria-label="Browse ${card.area().displayName()}"></a>
                        </div>
                    </div>
                </div>
            @endfor
        </div>
    </section>

    @if(!model.missingPublications().isEmpty())
        <section aria-labelledby="missing-books-heading">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h2 id="missing-books-heading" class="h4 mb-0">
                    <i class="bi bi-book me-2"></i>Available Sourcebooks
                </h2>
                <a href="/publications" class="btn btn-sm btn-link">View all publications</a>
            </div>
            <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-xl-4 g-3">
                @for(var pub : model.missingPublications())
                    <div class="col">
                        <div class="card h-100 shadow-sm">
                            <div class="card-body d-flex flex-column">
                                <h3 class="h6 card-title mb-1">
                                    <a href="${pub.detailPath()}" class="text-decoration-none">${pub.name()}</a>
                                </h3>
                                <div class="small text-muted mb-3">${pub.publisher()}</div>
                                @if(pub.storeUrl() != null)
                                    <div class="mt-auto">
                                        <a href="${pub.storeUrl()}" target="_blank" rel="noopener noreferrer"
                                           class="btn btn-sm btn-outline-primary w-100">
                                            <i class="bi bi-box-arrow-up-right me-1"></i>DriveThruRPG
                                        </a>
                                    </div>
                                @endif
                            </div>
                        </div>
                    </div>
                @endfor
            </div>
        </section>
    @endif
`)
