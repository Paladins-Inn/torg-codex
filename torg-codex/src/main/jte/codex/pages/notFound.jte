@param de.paladinsinn.torg.codex.web.codex.model.NotFoundViewModel model

@template.codex.layout.page(title = model.title(), nav = model.nav(), content = @`
    <div class="card border-0 shadow-sm mx-auto my-5" style="max-width: 36rem;">
        <div class="card-body text-center p-5">
            <i class="bi bi-compass fs-1 text-secondary d-block mb-3"></i>
            <h1 class="h4 mb-2">${model.title()}</h1>
            <p class="text-muted mb-4">${model.message()}</p>
            <a href="/" class="btn btn-primary">
                <i class="bi bi-house-door me-1"></i>Return to Codex Home
            </a>
        </div>
    </div>
`)
