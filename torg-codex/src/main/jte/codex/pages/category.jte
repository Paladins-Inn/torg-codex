@param de.paladinsinn.torg.codex.web.codex.model.TableViewModel model

@template.codex.layout.page(title = model.area().displayName(), nav = model.nav(), content = @`
    <header class="mb-4">
        <h1 class="h3 mb-1">
            <i class="bi ${model.area().icon()} me-2 text-primary"></i>${model.area().displayName()}
        </h1>
        <p class="text-muted mb-0">${model.area().description()}</p>
    </header>

    @template.codex.fragments.filterPanel(filter = model.filterPanel())

    <div id="catalog-results">
        @template.codex.fragments.categoryResults(model = model)
    </div>
`)
