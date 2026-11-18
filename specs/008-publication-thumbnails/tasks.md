---
description: "Implementation tasks for homepage publication thumbnails"
---

# Tasks: Homepage publication thumbnails

**Input:** [Specification](spec.md), [plan](plan.md), [research](research.md), [data model](data-model.md), [homepage contract](contracts/homepage.md), and [quickstart](quickstart.md).

**Format:** `Tnnn [P?] [USn?] Description with exact file path`. `[P]` indicates independent work on different files once the listed prerequisites are met. Write each story's automated checks before implementing its behavior, as required by the plan and constitution.

## Phase 1: Setup

- [X] T001 Create and check out the branch `008-publication-thumbnails`, then confirm JDK 25, a running Docker daemon, Chrome or Chromium for the automated browser checks, the Maven wrapper, and public test fixtures using `mvnw.cmd` and `specs\008-publication-thumbnails\quickstart.md`; use no real visitor credentials or live image requests in automated checks.

## Phase 2: Foundational

The existing domain model, homepage rendering infrastructure, and asset directories are sufficient. No new dependencies, configuration, schema, download step, or thumbnail index is needed.

- [X] T002 Establish the regression baseline by running the existing `torg-codex\src\test\java\de\paladinsinn\torg\codex\web\codex\CodexWebUiDifferentialTest.java` through the Maven reactor before altering homepage rendering.

## Phase 3: User Story 1 - Recognize publications by their title page (P1, MVP)

**Goal:** Existing Available Sourcebooks cards show the primary product's DriveThruRPG `thumb140` image without changing publication visibility, order, or links.

**Independent test:** Compare a known card's image to the provider thumbnail as a signed-out and signed-in visitor. At phone and desktop widths, verify that the same 140-pixel source is used and the whole cover fits without crowding out the card text.

- [X] T003 [P] [US1] Add offline tests in `torg-codex-domain\src\test\java\de\paladinsinn\torg\codex\domain\model\PublicationThumbnailTest.java` for the exact `216248-thumb140.jpg` URL, the matching `216248-thumb140.png` fallback URL (identical except for the extension), an arbitrary positive primary ID, a publication with alternate product IDs, nonpositive IDs, and unchanged full-cover and store links. Assert that the helpers are pure string functions: they return a link for an ID that exists nowhere and use no HTTP client.
- [X] T004 [P] [US1] Add rendered-homepage assertions in `torg-codex\src\test\java\de\paladinsinn\torg\codex\web\codex\HomePublicationThumbnailsTest.java` for the exact thumbnail source and PNG fallback attribute on cards common to anonymous and signed-in visitors, unchanged list order and membership, preserved names, publishers and links, and unchanged behavior when the list is empty. Render the homepage with no DriveThruRPG stub registered and assert that the backend attempts no outbound request.
- [X] T005 [US1] Add pure thumbnail URL helpers to `torg-codex-domain\src\main\java\de\paladinsinn\torg\codex\domain\model\Publication.java`: reuse the existing DriveThruRPG image base, append `{primaryProductId}-thumb140.jpg` (primary) and `{primaryProductId}-thumb140.png` (fallback) for positive IDs, and return no URL for nonpositive IDs. Leave the existing cover and store helpers unchanged; do not make HTTP calls.
- [X] T006 [US1] Add nullable `thumbnailUrl` and `thumbnailFallbackUrl` to the homepage card and populate them using the domain helpers in `torg-codex\src\main\java\de\paladinsinn\torg\codex\web\codex\model\HomeViewModel.java` and `torg-codex\src\main\java\de\paladinsinn\torg\codex\web\codex\mapper\CodexViewMapper.java`; preserve existing selection and full-cover consumers. Depends on T005.
- [X] T007 [US1] Render the thumbnail in the existing card loop in `torg-codex\src\main\jte\codex\pages\home.jte`; emit an escaped `img` only when `thumbnailUrl` exists, with `thumbnailFallbackUrl` in an escaped `data-fallback-src` attribute, use the publication name as alt text, lazy-load the image, and set `referrerpolicy="no-referrer"`. Do not emit `srcset` alternatives or add a new publication section. Depends on T006.
- [X] T008 [P] [US1] Style the agreed `.publication-thumbnail` frame and image in `torg-codex\src\main\resources\static\themes\codex\codex.css`: reserve a 140 by 190 CSS-pixel frame, shrink proportionally if needed, and contain the whole image without stretching or cropping. Can run alongside T007 after agreeing on the class.

## Phase 4: User Story 2 - Reach the publication through its thumbnail (P2)

**Goal:** The cover and title lead to the same Codex detail page through one accessible link; the store button remains separate.

**Independent test:** Click the image, tap it, and activate the detail link by keyboard. All three open the same publication as the title. Tab and screen-reader navigation expose one named detail link per card and retain the independent store link.

- [X] T009 [US2] Extend `torg-codex\src\test\java\de\paladinsinn\torg\codex\web\codex\HomePublicationThumbnailsTest.java` to assert that the thumbnail and visible title share exactly one detail link with an accessible name from the title, that alt text names the publication, and that the optional store anchor stays separate.
- [X] T010 [US2] Wrap the thumbnail frame and title in a single existing-detail-path anchor in `torg-codex\src\main\jte\codex\pages\home.jte`; reference the visible title for the accessible link name, maintain correct heading structure and keyboard focus, and keep the store button outside that anchor.

## Phase 5: User Story 3 - A tidy homepage when a thumbnail is missing (P3)

**Goal:** Missing, delayed, or broken thumbnails show the same neutral frame without blocking the rest of the homepage or moving content.

**Independent test:** Compare a card without a product ID to one whose image is blocked or delayed. Both show the same frame and remain usable; the card without an ID sends no image request. A successful image replaces the placeholder without changing dimensions.

- [X] T011 [US3] Extend `torg-codex\src\test\java\de\paladinsinn\torg\codex\web\codex\HomePublicationThumbnailsTest.java` to assert that a card with no thumbnail URL gets the neutral frame but no `img`, and that a card with a URL retains the frame before successful loading; confirm that title, publisher and applicable links remain present. Add `torg-codex\src\test\java\de\paladinsinn\torg\codex\web\codex\PublicationThumbnailsBrowserTest.java`, reusing the headless-browser approach in `torg-codex\src\test\java\de\paladinsinn\torg\codex\web\notification\ToastBrowserTest.java`, to execute the shipped thumbnail script with controlled image outcomes: JPEG success without a PNG attempt, JPEG failure followed by PNG success, both failing without another source attempt, and cached JPEG success or failure before script initialization. Assert the visible image or placeholder and application-controlled source attempts, not just generated HTML; exclude redirect hops from the attempt count and never contact the live provider. While image loading remains pending or blocked, assert that homepage text, category navigation and publication/store links remain visible and usable.
- [X] T012 [P] [US3] Implement browser load-state handling in `torg-codex\src\main\resources\static\js\publication-thumbnails.js`: add listeners before checking completion; reveal only successfully loaded images, including warm-cache hits. On the first error, consume and remove `data-fallback-src` before assigning its value as the image source so no second switch can happen; on a further error keep the neutral frame. Keep the neutral frame for pending, blocked or undecodable images. Use `.publication-thumbnail` and a shared `is-loaded` class; never build URLs in the script. Allow at most two application-controlled image-source attempts per card, JPEG followed by PNG; exclude HTTP redirect hops and never retry either format.
- [X] T013 [P] [US3] Add neutral placeholder styling and keep the image in layout but transparent (`opacity: 0`; never `display: none` or `visibility: hidden`, so lazy loading still fetches it) until `.publication-thumbnail.is-loaded` in `torg-codex\src\main\resources\static\themes\codex\codex.css`, preserving the frame size and the active detail link in every state.
- [X] T014 [US3] Render the frame regardless of thumbnail availability and wire the local script with a `<script src="/js/publication-thumbnails.js" defer>` tag at the end of the page content in `torg-codex\src\main\jte\codex\pages\home.jte` (not in the shared layout `page.jte`, so other reader pages do not load it); keep it off the admin surface and avoid inline event handlers. Depends on T012 and T013.

## Phase 6: Polish & cross-cutting concerns

- [X] T015 [P] Document browser-only thumbnail loading, direct `thumb140` links derived from the primary ID, the single JPEG-to-PNG fallback in the browser, and the placeholder behavior in `docs\modules\arc42\pages\08_concepts\jte-with-markdown.adoc`; clarify that there is no image download, probe, or metadata lookup during build, startup, or homepage rendering.
- [X] T016 Run `specs\008-publication-thumbnails\quickstart.md` from `mvnw.cmd`, including focused tests and reactor verification; confirm that `PublicationThumbnailsBrowserTest` executes rather than being skipped. Supplement the automated cases with browser network controls, keyboard and screen-reader checks, frame measurements, and no server-side thumbnail traffic. Confirm that a PNG-only title (for example product `260070`) shows its thumbnail after one failed JPEG source attempt, and that a blocked image origin causes at most two application-controlled image-source attempts per card; exclude HTTP redirect hops and verify neither format is retried. Confirm that a thumbnail below the fold loads when scrolled into view, that homepage text and links remain usable while loading is pending or blocked, and that with JavaScript disabled the cards show the placeholder and stay usable.

## Dependencies & execution order

- **Phases:** Setup → foundational baseline → US1 → US2 → US3 → final verification. US2 and US3 share the rendered card and its test file, so integrate those edits in order. Verify each story with its independent test before advancing.
- **US1:** T003 and T004 in parallel, then T005 → T006 → T007; T008 can run beside T007.
- **US2:** T009 → T010. These two tasks are test-first and sequential; browser navigation checks can run independently across mouse/touch and keyboard after T010.
- **US3:** After T011, T012 and T013 can run in parallel, then T014. T015 can run alongside T016 once the behavior is wired.

## Implementation strategy

Deliver US1 first as the demonstrable MVP: existing homepage cards with official 140-pixel thumbnails and unchanged list behavior. Add single-link navigation and accessible naming in US2, then the stable placeholder and asynchronous image states in US3. Complete all three before release so slow or unavailable images do not degrade the homepage.
