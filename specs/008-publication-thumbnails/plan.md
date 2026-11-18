# Implementation Plan: Homepage publication thumbnails

**Feature (setup branch):** `008-publication-thumbnails` | **Checkout:** `main` | **Date:** 2026-11-18 | **Input:** [spec.md](spec.md)

## Summary

Add a DriveThruRPG thumbnail to each existing Available Sourcebooks card. Derive a thumbnail link from the publication's existing DriveThruRPG image base and positive primary product ID (`{id}-thumb140.jpg`), plus a `.png` variant of the same link. Only the browser requests the image: JPEG first, PNG once if the JPEG fails, then the placeholder. Keep the existing full-cover URL unchanged for its other consumers.

## Technical Context

- **Platform:** Java 25, Maven reactor, Spring Boot, JTE, Bootstrap, reader-scoped CSS and a small local script. No new runtime dependency.
- **Storage:** No index, packaged images, image cache or database change.
- **Scope and cost:** Local URL formatting only; no thumbnail or metadata requests during build, startup or homepage rendering. Lazy load offscreen images, reserve their dimensions, and preserve the existing order and links on phones and desktops.
- **Testing:** JUnit and AssertJ for link helpers and rendered HTML, plus automated headless-browser tests executing the shipped thumbnail script using the existing `ToastBrowserTest` approach. Cover JPEG success, PNG fallback, both failing, and cached completion before initialization with controlled image outcomes and no live provider calls. If provider HTTP responses are simulated, follow the repository's WireMock annotation and file-fixture conventions. Count application-controlled image-source attempts, not HTTP redirect hops. Supplement these tests with browser network controls, layout and accessibility checks. Run the existing full reactor gate before merge.

## Constitution Check

| Principles | Before research | After design |
|---|---|---|
| I-II: module ownership and integration | Keep provider details out of templates and database adapters. | Pass: pure link derivation beside the existing publication link; no new service or server integration. |
| III-IV: REST and schema | Presentation change only. | Pass: no REST fields, endpoints, tables or changesets change. |
| V-VI: ownership, privacy and rendering | Preserve publication selection and censorship. | Pass: only public image references, escaped attributes, no sensitive parameters or Codex referrer. No protected text or new ownership mechanism. |
| VII-VIII: operations and testing | Preserve request metrics and test offline first. | Pass by design: no new service to monitor; resolver, rendering, failure and ownership regression checks specified below. |

No constitutional exception or new architecture-level decision is required. Reader documentation should explain the browser's direct image request.

## Project Structure

| Location | Responsibility |
|---|---|
| `torg-codex-domain/src/main/` | Add a pure thumbnail link helper beside `Publication.coverURL()`, reusing the existing image base. |
| `torg-codex/src/main/java/.../web/codex/` | Extend `HomeViewModel.PublicationCard` and its assembly in `CodexViewMapper`; keep full-cover consumers unchanged. |
| `torg-codex/src/main/jte/codex/pages/home.jte` | Existing card loop, one detail link, reserved thumbnail frame, independent store link. |
| `torg-codex/src/main/resources/static/` | Reader styling in `themes/codex/codex.css`; local image-state behavior under `js/`. |
| Domain and web `src/test/` | Offline URL derivation, rendered HTML, `PublicationThumbnailsBrowserTest` for executed fallback behavior, and existing DRM regression tests. |

## Delivery order

1. Write failing link and homepage tests for the fixture ID, an arbitrary new ID and a nonpositive ID. No live DriveThruRPG calls are needed.
2. Wire the nullable thumbnail into existing cards without touching ownership, catalog order, REST or purchase teasers. Implement the rendering and error states in the [homepage contract](contracts/homepage.md).
3. Run the [quickstart](quickstart.md), including the automated browser tests, offline startup, keyboard, delayed-image and blocked-image cases. Verify homepage text and links while images remain pending, rather than relying on a subjective slowdown comparison.

**Design artifacts:** [Research](research.md) and [data model](data-model.md). Implementation tasks are deferred to `/speckit-tasks`.
