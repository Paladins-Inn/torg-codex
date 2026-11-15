@param de.paladinsinn.torg.codex.web.codex.model.SanitizedHtml content

@if(content != null && !content.isEmpty())
    <div class="codex-prose">$unsafe{content.html()}</div>
@endif
