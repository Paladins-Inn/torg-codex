@param de.paladinsinn.torg.codex.domain.catalog.CatalogArea area
@param int count
@param String href = null
@param String ariaLabel = null

<div class="col">
    <div class="card h-100 shadow-sm position-relative">
        <div class="card-body d-flex flex-column">
            <div class="d-flex justify-content-between align-items-start mb-2">
                <h3 class="h6 card-title mb-0">
                    <i class="bi ${area.icon()} me-2 text-primary"></i>${area.displayName()}
                </h3>
                <span class="badge rounded-pill text-bg-light border">${count}</span>
            </div>
            <p class="card-text small text-muted mb-0 flex-grow-1">${area.description()}</p>
            @if(href != null)
                <a href="${href}" class="stretched-link"
                   aria-label="${ariaLabel != null ? ariaLabel : "Browse " + area.displayName()}"></a>
            @endif
        </div>
    </div>
</div>
