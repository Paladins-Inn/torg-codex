@param de.paladinsinn.torg.codex.web.codex.model.DetailViewModel model

@template.codex.layout.page(title = model.header().name(), nav = model.nav(), content = @`
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb small mb-0">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item"><a href="${model.area().canonicalPath()}">${model.area().displayName()}</a></li>
            <li class="breadcrumb-item active" aria-current="page">${model.header().name()}</li>
        </ol>
    </nav>

    <header class="d-flex flex-wrap justify-content-between align-items-center gap-2 pb-3 mb-4 border-bottom">
        <h1 class="h2 mb-0">${model.header().name()}</h1>
        <div class="d-flex flex-wrap align-items-center gap-2">
            @template.codex.fragments.badges(
                cosms = model.header().cosms(),
                clearanceSymbol = model.header().clearanceSymbol(),
                clearanceName = model.header().clearanceName()
            )
        </div>
    </header>

    <div class="row g-4">
        <div class="${model.attributeGroups().isEmpty() ? "col-12" : "col-12 col-lg-8"}">
            @for(var prose : model.proseSections())
                <section class="card border-0 shadow-sm mb-3">
                    <div class="card-body">
                        @if(prose.title() != null && !prose.title().isEmpty())
                            <h2 class="h5 card-title mb-3">${prose.title()}</h2>
                        @endif
                        @template.codex.fragments.richText(content = prose.content())
                    </div>
                </section>
            @endfor

            @for(var embedded : model.embeddedGroups().entrySet())
                <section class="card border-0 shadow-sm mb-3">
                    <div class="card-header bg-white fw-semibold">${embedded.getKey()}</div>
                    <ul class="list-group list-group-flush">
                        @for(var ref : embedded.getValue())
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                <a href="${ref.detailPath()}" class="fw-medium text-decoration-none">${ref.name()}</a>
                                <span class="badge text-bg-light border">${ref.targetArea().singularName()}</span>
                            </li>
                        @endfor
                    </ul>
                </section>
            @endfor
        </div>

        @if(!model.attributeGroups().isEmpty())
            <aside class="col-12 col-lg-4">
                @for(var group : model.attributeGroups())
                    <div class="card shadow-sm mb-3">
                        <div class="card-header bg-light fw-semibold">${group.title()}</div>
                        <dl class="card-body mb-0 row g-2 small">
                            @for(var item : group.items())
                                <dt class="col-5 text-muted">${item.label()}</dt>
                                <dd class="col-7 mb-0">
                                    @if(item.markupValue() != null)
                                        @template.codex.fragments.richText(content = item.markupValue())
                                    @else
                                        ${item.plainValue()}
                                    @endif
                                </dd>
                            @endfor
                        </dl>
                    </div>
                @endfor
            </aside>
        @endif
    </div>

    @if(!model.publications().isEmpty())
        <footer class="mt-4 pt-3 border-top small text-muted">
            <i class="bi bi-book me-1"></i>Source publication(s):
            @for(int i = 0; i < model.publications().size(); i++)
                !{var pub = model.publications().get(i);}
                @if(i > 0), @endif
                @if(pub.detailPath() != null)
                    <a href="${pub.detailPath()}" class="text-decoration-none">${pub.name()}</a>
                @else
                    <span>${pub.name()}</span>
                @endif
                @if(pub.storeUrl() != null)
                    (<a href="${pub.storeUrl()}" target="_blank" rel="noopener noreferrer">DriveThruRPG</a>)
                @endif
            @endfor
        </footer>
    @endif
`)
