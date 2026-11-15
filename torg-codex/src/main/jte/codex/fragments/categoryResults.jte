@import de.paladinsinn.torg.codex.domain.catalog.ColumnDescriptor.CellKind
@param de.paladinsinn.torg.codex.web.codex.model.TableViewModel model

<div class="card shadow-sm">
    <div class="card-header bg-white d-flex flex-wrap justify-content-between align-items-center py-2">
        <span class="small text-muted">
            Showing <strong>${model.matchingCount()}</strong> of <strong>${model.totalAccessibleCount()}</strong>
            accessible ${model.area().displayName().toLowerCase()}
        </span>
        @if(model.filterPanel().hasActiveFilters())
            <a href="${model.area().canonicalPath()}" class="small text-decoration-none">
                <i class="bi bi-x-circle me-1"></i>Clear active filters
            </a>
        @endif
    </div>

    <div class="table-responsive">
        <table class="table table-striped table-hover align-middle codex-table mb-0">
            <thead class="table-light">
            <tr>
                @for(var col : model.headers())
                    <th scope="col">
                        @if(col.sortable())
                            <a href="${col.sortUrl()}"
                               hx-get="${col.sortUrl()}"
                               hx-target="#catalog-results"
                               hx-push-url="true"
                               class="link-dark text-decoration-none d-inline-flex align-items-center gap-1">
                                <span>${col.label()}</span>
                                @if(col.activeSort())
                                    <i class="bi ${col.ascending() ? "bi-sort-alpha-down" : "bi-sort-alpha-up"} text-primary"></i>
                                @else
                                    <i class="bi bi-arrow-down-up text-muted opacity-50 small"></i>
                                @endif
                            </a>
                        @else
                            <span>${col.label()}</span>
                        @endif
                    </th>
                @endfor
            </tr>
            </thead>
            <tbody>
            @if(model.rows().isEmpty())
                <tr>
                    <td colspan="${model.headers().size()}" class="text-center py-5 text-muted">
                        <i class="bi bi-inbox fs-3 d-block mb-2"></i>
                        <p class="mb-2">No matching entries found for the selected criteria.</p>
                        <a href="${model.area().canonicalPath()}" class="btn btn-sm btn-outline-secondary">
                            Reset filters
                        </a>
                    </td>
                </tr>
            @else
                @for(var row : model.rows())
                    <tr>
                        @for(var cell : row.cells())
                            <td>
                                @if(cell.kind() == CellKind.PRIMARY_LINK)
                                    <a href="${cell.href()}" class="fw-semibold text-decoration-none">${cell.text()}</a>
                                @elseif(cell.kind() == CellKind.MARKUP)
                                    @template.codex.fragments.richText(content = cell.markupValue())
                                @elseif(cell.kind() == CellKind.CLEARANCE_BADGE)
                                    @if(!cell.text().isEmpty())
                                        <span class="badge text-bg-dark clearance-badge">${cell.text()}</span>
                                    @endif
                                @elseif(cell.kind() == CellKind.COSM_BADGES)
                                    @for(var badge : cell.cosmBadges())
                                        <a href="/cosms/${badge.slug()}" class="${badge.cssClass()} text-decoration-none me-1">${badge.label()}</a>
                                    @endfor
                                @elseif(cell.kind() == CellKind.GROUP_BADGE)
                                    @if(!cell.text().isEmpty())
                                        <span class="badge text-bg-secondary">${cell.text()}</span>
                                    @endif
                                @elseif(cell.kind() == CellKind.BOOLEAN_FLAG)
                                    @if(!cell.text().isEmpty())
                                        <span class="badge text-bg-warning"><i class="bi bi-check-lg"></i></span>
                                    @endif
                                @elseif(cell.kind() == CellKind.EXTERNAL_LINK)
                                    @if(!cell.href().isEmpty())
                                        <a href="${cell.href()}" target="_blank" rel="noopener noreferrer"
                                           class="btn btn-sm btn-outline-primary py-0 px-2">
                                            <i class="bi bi-box-arrow-up-right me-1"></i>${cell.text()}
                                        </a>
                                    @endif
                                @else
                                    ${cell.text()}
                                @endif
                            </td>
                        @endfor
                    </tr>
                @endfor
            @endif
            </tbody>
        </table>
    </div>
</div>
