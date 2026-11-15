@param de.paladinsinn.torg.codex.web.codex.model.FilterPanelViewModel filter

<form id="catalog-filter-form"
      class="card shadow-sm mb-4"
      action="${filter.actionPath()}"
      method="get"
      hx-get="${filter.actionPath()}"
      hx-target="#catalog-results"
      hx-push-url="true"
      hx-indicator="#catalog-loading"
      hx-trigger="change, submit">
    <input type="hidden" name="sort" value="${filter.sortKey()}">
    <input type="hidden" name="dir" value="${filter.sortDir()}">

    <div class="card-body">
        <div class="row g-3 align-items-end">
            <div class="col-12 col-md-5">
                <label for="filter-q" class="form-label small fw-semibold mb-1">
                    <i class="bi bi-search me-1"></i>Search by Name
                </label>
                <input id="filter-q"
                       type="search"
                       name="q"
                       value="${filter.searchQuery()}"
                       class="form-control form-control-sm"
                       placeholder="Type to filter entries..."
                       hx-get="${filter.actionPath()}"
                       hx-target="#catalog-results"
                       hx-include="#catalog-filter-form"
                       hx-push-url="true"
                       hx-indicator="#catalog-loading"
                       hx-trigger="input changed delay:200ms, search">
            </div>

            @for(var group : filter.groupingFacets())
                @if(group.options().size() > 6)
                    <div class="col-12 col-sm-6 col-md-3">
                        <label for="facet-${group.dimension().getTag()}" class="form-label small fw-semibold mb-1">
                            ${group.label()}
                        </label>
                        <select id="facet-${group.dimension().getTag()}"
                                name="${group.queryParam()}"
                                class="form-select form-select-sm">
                            <option value="">All ${group.label()}s</option>
                            @for(var opt : group.options())
                                <option value="${opt.key()}" selected="${opt.selected()}">${opt.label()}</option>
                            @endfor
                        </select>
                    </div>
                @endif
            @endfor

            <div class="col-auto ms-auto d-flex align-items-center gap-2">
                <span id="catalog-loading" class="htmx-indicator spinner-border spinner-border-sm text-primary"
                      role="status" aria-label="Loading"></span>
                <noscript>
                    <button type="submit" class="btn btn-sm btn-primary">Apply</button>
                </noscript>
                <a href="${filter.actionPath()}" class="btn btn-sm btn-outline-secondary">
                    <i class="bi bi-arrow-counterclockwise me-1"></i>Reset
                </a>
            </div>
        </div>

        @if(!filter.cosms().isEmpty() || filter.groupingFacets().stream().anyMatch(g -> g.options().size() <= 6))
            <hr class="my-3">
            <div class="row g-3">
                @for(var group : filter.groupingFacets())
                    @if(group.options().size() <= 6)
                        <div class="col-12 col-md-4">
                            <div class="small fw-semibold mb-1">${group.label()}</div>
                            <div class="d-flex flex-wrap gap-2">
                                @for(var opt : group.options())
                                    <div class="form-check form-check-inline mb-0">
                                        <input class="form-check-input"
                                               type="checkbox"
                                               id="chk-${group.dimension().getTag()}-${opt.key()}"
                                               name="${group.queryParam()}"
                                               value="${opt.key()}"
                                               checked="${opt.selected()}">
                                        <label class="form-check-label small"
                                               for="chk-${group.dimension().getTag()}-${opt.key()}">${opt.label()}</label>
                                    </div>
                                @endfor
                            </div>
                        </div>
                    @endif
                @endfor

                @if(!filter.cosms().isEmpty())
                    <div class="col-12 col-md-8">
                        <div class="small fw-semibold mb-1">Cosm</div>
                        <div class="d-flex flex-wrap gap-2">
                            @for(var cosm : filter.cosms())
                                !{var badge = de.paladinsinn.torg.codex.web.codex.model.CosmBadgeViewModel.of(cosm.key(), cosm.label());}
                                <div class="form-check form-check-inline mb-0">
                                    <input class="form-check-input"
                                           type="checkbox"
                                           id="chk-cosm-${cosm.key()}"
                                           name="cosm"
                                           value="${cosm.key()}"
                                           checked="${cosm.selected()}">
                                    <label class="form-check-label small" for="chk-cosm-${cosm.key()}">
                                        <span class="${badge.cssClass()}">${badge.label()}</span>
                                    </label>
                                </div>
                            @endfor
                        </div>
                    </div>
                @endif
            </div>
        @endif
    </div>
</form>
