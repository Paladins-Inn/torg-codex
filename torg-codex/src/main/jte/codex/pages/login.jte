@param de.paladinsinn.torg.codex.web.codex.model.AuthPageViewModel model

@template.codex.layout.page(title = "Sign In", nav = model.nav(), content = @`
    <div class="row justify-content-center my-4">
        <div class="col-12 col-md-8 col-lg-5">
            @if(model.signedOutBanner())
                <div class="alert alert-info d-flex align-items-center" role="status">
                    <i class="bi bi-info-circle-fill me-2"></i>
                    <div>You have signed out. You are now browsing free-tier Core Rulebook content.</div>
                </div>
            @endif

            @if(model.errorMessage() != null && !model.errorMessage().isBlank())
                <div class="alert alert-danger d-flex align-items-center" role="alert">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    <div>${model.errorMessage()}</div>
                </div>
            @endif

            <div class="card border-0 shadow-sm">
                <div class="card-header bg-dark text-white py-3">
                    <h1 class="h5 mb-0">
                        <i class="bi bi-key-fill me-2 text-warning"></i>DriveThruRPG Library Sign In
                    </h1>
                </div>
                <div class="card-body p-4">
                    <p class="text-muted small mb-4">
                        Enter your personal DriveThruRPG Application Key to unlock all Torg: Eternity publications in your DriveThruRPG library for this session.
                    </p>
                    <form action="/login" method="post">
                        <div class="mb-3">
                            <label for="apiKey" class="form-label fw-semibold">DriveThruRPG Application Key</label>
                            <input type="password"
                                   class="form-control"
                                   id="apiKey"
                                   name="apiKey"
                                   placeholder="Enter your DriveThruRPG API key"
                                   autocomplete="off"
                                   required>
                        </div>
                        <div class="d-grid gap-2">
                            <button type="submit" class="btn btn-warning fw-semibold">
                                <i class="bi bi-unlock-fill me-1"></i>Unlock Library
                            </button>
                            <a href="/" class="btn btn-outline-secondary">Continue with Free Tier</a>
                        </div>
                    </form>
                </div>
                <div class="card-footer bg-light small text-muted py-3">
                    <i class="bi bi-question-circle me-1"></i>Where do I find my key? Open your
                    <a href="https://www.drivethrurpg.com/account_edit.php" target="_blank" rel="noopener noreferrer">
                        DriveThruRPG Account Settings
                    </a>
                    and generate or copy an Application Key.
                </div>
            </div>
        </div>
    </div>
`)
