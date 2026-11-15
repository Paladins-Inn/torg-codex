@param java.util.List<de.paladinsinn.torg.codex.web.codex.model.CosmBadgeViewModel> cosms = java.util.List.of()
@param String clearanceSymbol = null
@param String clearanceName = null

@if(clearanceSymbol != null && !clearanceSymbol.isBlank())
    <span class="badge text-bg-dark clearance-badge"
          title="${clearanceName != null ? "Clearance: " + clearanceName : "Clearance"}">${clearanceSymbol}</span>
@endif
@for(var cosm : cosms)
    <a href="/cosms/${cosm.slug()}" class="${cosm.cssClass()} text-decoration-none me-1">${cosm.label()}</a>
@endfor
