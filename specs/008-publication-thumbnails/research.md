# Research: homepage publication thumbnails

## Which image belongs on the card?

**Decision:** Use the public 140-pixel thumbnail link for the primary DriveThruRPG product. If unavailable, show the placeholder; do not probe other sizes or fall back to the full cover.

**Evidence:** `torg-codex/src/test/resources/__files/drivethru/product-216248-response.json` distinguishes `image` (`3444/216248.jpg`) from `thumbnail` (`3444/216248-thumb140.jpg`) and the 100-, 80- and 40-pixel variants. `Product.java` in the DriveThruRPG application integration resolves relative image paths against `https://api.drivethrurpg.com/images/`. The fixture establishes the distinction for one product, not a universal filename rule or proof of current CDN availability.

**Alternative rejected:** `HomeViewModel.PublicationCard.coverUrl` looks convenient but contains the full cover. Resizing it in the browser still requests the wrong asset.

## How can signed-out visitors see the correct product without a network dependency on the homepage?

**Decision:** Follow the existing `Publication.coverURL()` convention: preserve its DriveThruRPG origin and image directory, using `{primaryProductId}-thumb140.jpg` instead of `{primaryProductId}.jpg`. For `216248` the result is `https://www.drivethrurpg.com/images/3444/216248-thumb140.jpg`. This constructs a link, not an HTTP request. A second link with the `.png` extension is derived the same way and used by the browser only if the JPEG fails.

**Live check (2026-11-18):** The referenced `dtrpg-api` OpenAPI specification documents no product endpoint or image fields. The public endpoint `GET /api/vBeta/products/{id}` was queried without credentials for all 69 primary product IDs in `torg_publication.csv`. Every product returns `thumbnail` as `{publisherId}/{productId}-thumb140.{ext}`; the publisher ID is `3444` for all 69, including the Infiniverse titles. 52 thumbnails are `.jpg` and 17 are `.png` (for example `3444/260070-thumb140.png`). The links resolve on `www.drivethrurpg.com/images/`, and a missing image returns `403`.

**Rationale:** The existing publication image link supplies the base and directory, which the live check confirms for the whole current catalog. A new positive ID works without registration. The extension is the only part the publication data cannot supply, so the browser tries the majority format, JPEG, first and PNG once. If the majority ever shifts to PNG, swap the two links.

**Alternatives rejected:** Storing the image extension per publication needs a schema change and a value for every new publication. A per-product index duplicates existing link information. Build or startup downloads, background warming, server-side probes and product metadata lookups all add a dependency on the store. Only the browser attempts the image request.

## How should image loading affect the card?

**Decision:** Render a local neutral frame first. Reveal the remote image only after successful loading; a local reader script handles both cached completions and later load/error events. On the first error the script switches the image once to the PNG link the server rendered alongside it; a second error leaves the neutral frame. Use one detail link for image and title, with its accessible name taken from the visible title. The store link remains separate.

**Alternatives rejected:** An inline error handler can be blocked by content security policy, an unbounded source change on error can loop (the PNG fallback is therefore tried exactly once), and separate cover/title links add a redundant keyboard stop. Neither image failure nor disabled JavaScript should prevent navigation.
