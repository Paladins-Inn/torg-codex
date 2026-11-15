@param de.paladinsinn.torg.codex.web.codex.model.AuthPageViewModel model

@template.codex.layout.page(title = "Sign Out", nav = model.nav(), content = @`
    <div class="row justify-content-center my-5">
        <div class="col-12 col-md-8 col-lg-5">
            <div class="card border-0 shadow-sm">
                <div class="card-header bg-dark text-white py-3">
                    <h1 class="h5 mb-0">
                        <i class="bi bi-box-arrow-right me-2 text-warning"></i>Sign Out of Torg Codex
                    </h1>
                </div>
                <div class="card-body p-4">
                    @if(model.nav().signedIn())
                        <p class="text-muted mb-4">
                            Signing out clears your active DriveThruRPG library session on this browser and returns the codex to free-tier Core Rulebook access.
                        </p>
                        <form action="/logout" method="post" class="d-grid gap-2">
                            <button type="submit" class="btn btn-danger">
                                <i class="bi bi-box-arrow-right me-1"></i>Sign Out
                            </button>
                            <a href="/" class="btn btn-outline-secondary">Cancel and Return to Codex</a>
                        </form>
                    @else
                        <p class="text-muted mb-4">
                            You are currently not signed in and are already browsing free-tier Core Rulebook content.
                        </p>
                        <div class="d-grid gap-2">
                            <a href="/login" class="btn btn-warning">
                                <i class="bi bi-key-fill me-1"></i>Sign In
                            </a>
                            <a href="/" class="btn btn-outline-secondary">Return to Codex Home</a>
                        </div>
                    @endif
                </div>
            </div>
        </div>
    </div>
`)
