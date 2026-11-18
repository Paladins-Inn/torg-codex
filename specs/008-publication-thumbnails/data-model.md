# Data model

| Object | Fields and relationships | Rules |
|---|---|---|
| Publication (existing) | Primary product ID, related product IDs, name, publisher, Codex detail path | Only the primary ID selects the picture. No persistent fields change. |
| Public thumbnail links (derived, not stored) | Existing DriveThruRPG image directory plus `{primaryProductId}-thumb140.jpg` (primary) and `{primaryProductId}-thumb140.png` (fallback) | Format both for any positive primary ID. Do not probe either URL or store image bytes or the extension. |
| Homepage card (existing view model) | Add nullable `thumbnailUrl` and `thumbnailFallbackUrl`; retain name, publisher, detail path and optional store URL | Missing or nonpositive ID yields neither URL. `coverUrl` is not a fallback. |

Availability is known only when the visitor's browser tries to load the image; no availability flag or mapping is maintained.

**Display states:** No reference starts and stays in the neutral frame. A valid reference starts with the same frame; successful decoding displays the thumbnail. A first error switches once to the PNG link; a second error leaves the neutral frame visible for good. Neither transition changes frame dimensions, link destination, focus or publication visibility.
