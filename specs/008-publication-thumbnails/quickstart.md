# Validate homepage publication thumbnails

Run after implementation from the repository root with JDK 25, Docker running, and Chrome or Chromium available for the automated browser checks. Use the public fixture catalog, not a private library or real visitor key.

```powershell
.\mvnw.cmd -pl torg-codex -am "-Dtest=PublicationThumbnailTest,HomePublicationThumbnailsTest,PublicationThumbnailsBrowserTest" -Dsurefire.failIfNoSpecifiedTests=false test
.\mvnw.cmd -pl torg-codex -am test
.\mvnw.cmd clean verify
```

Block outbound DriveThruRPG traffic from the build and application processes (leave dependency repositories and local test services available). Check the request log: there must be zero thumbnail or metadata requests from this feature during build, startup or `GET /`. URL tests should use the existing publication image base and the fixture's thumbnail filename for `216248`. An arbitrary new positive ID must yield its own link without registration; a nonpositive ID yields none. The PNG fallback link must differ from the primary link only in its extension. For multiple editions, assert that the primary ID is used. Homepage tests should verify anonymous and signed-in lists, an empty list and unchanged detail/store destinations.

Confirm that `PublicationThumbnailsBrowserTest` executes rather than being skipped. It must run the shipped script and verify JPEG success without fallback, JPEG failure followed by PNG success, both failing without another source attempt, and cached JPEG success or failure before initialization. Check the visible image or placeholder, and verify that homepage text and navigation remain usable while images are pending or blocked. Use controlled image outcomes, not live provider requests.

**Interactive check:** With the project's local PostgreSQL configuration and schema already provisioned, start the reader:

```powershell
.\mvnw.cmd -pl torg-codex -am install -DskipTests
.\mvnw.cmd -pl torg-codex spring-boot:run
```

1. Open `http://localhost:8080/` at phone and desktop widths. In Available Sourcebooks, compare a known card to its store thumbnail. Check that the entire cover fits. Ask a reader familiar with a pictured book to locate it by cover in under five seconds.
2. Tab through a card and listen with a screen reader. There should be one named detail link, not two. Activate the image and the title separately; both open that publication. The store link still opens DriveThruRPG.
3. Disable browser caching and hold the image requests pending. Without releasing them, verify that homepage text and category navigation are visible, and that category, publication and store links can be activated. Repeat with image requests blocked. Record the frame dimensions before and after a successful image load; they must not change. Check network details for thumbnail-sized requests and absence of a Codex referrer.
4. With JavaScript enabled, find a card whose thumbnail is a PNG (for example "The Storm Knights' Guide to Monster Hunters", product `260070`). Its network log must show a failed JPEG source attempt followed by a successful PNG source attempt, and the thumbnail must appear. Each source attempt may follow HTTP redirects; do not count those hops as additional source attempts.
5. Block thumbnail requests and reload: each card with a product makes at most two application-controlled image-source attempts, one JPEG and one PNG, with no retry of either format. Exclude HTTP redirect hops from that count. Then test a card without an image reference. Both show the same neutral frame with no broken-image symbol; the latter sends no image request. Unblock, reload with warm cache and confirm images reappear. Finally, disable JavaScript and verify that cards show placeholders while all other homepage content and navigation remain usable.

Automated tests never contact the live store. Browser network controls supplement, rather than replace, the automated script checks. If provider HTTP responses are simulated, use the repository's WireMock annotation and file-fixture conventions. See the [UI contract](contracts/homepage.md) for expected results.
