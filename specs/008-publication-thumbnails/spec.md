# Feature Specification: Publication Title-Page Thumbnails on the Homepage

**Feature Branch**: `008-publication-thumbnails`

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "on the homepage show the title page as thumbnail for the publications. Use the drivethrurpg thumbnails"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recognize Publications by Their Title Page (Priority: P1)

A visitor opens the Torg Codex homepage and looks at the publications listed there. Today each publication appears as a text-only card with its name and publisher. With this feature every publication card shows a small picture of the publication's title page, taken from the thumbnail DriveThruRPG provides for that product. The visitor recognizes a book by its cover at a glance, exactly as they know it from their DriveThruRPG library or their bookshelf.

**Why this priority**: Showing the title page is the entire purpose of the feature. Covers are how players and game masters recognize Torg Eternity books; a text-only list forces them to read every title.

**Independent Test**: With JavaScript enabled, open the homepage as a visitor for whom at least one publication is listed and verify that each listed publication with an available DriveThruRPG thumbnail shows it next to its name and publisher. Verify unavailable thumbnails and disabled JavaScript separately using the documented placeholder cases.

**Acceptance Scenarios**:

1. **Given** JavaScript is enabled and the homepage lists a publication with an available DriveThruRPG thumbnail, **When** the visitor views the homepage, **Then** the publication's card shows that product's title-page thumbnail together with the publication name and publisher.
2. **Given** the homepage lists several publications, **When** the visitor views the homepage, **Then** all thumbnails are shown in a uniform size and aspect ratio so that the cards line up evenly in the grid.
3. **Given** a visitor who is not signed in and a visitor who is signed in, both with JavaScript enabled, **When** each views the homepage, **Then** both see available thumbnails on the publications listed for them; seeing a thumbnail does not require owning the publication, and unavailable thumbnails show the same placeholder for both visitors.
4. **Given** the homepage is viewed on a narrow (phone) and on a wide (desktop) screen, **When** the publication list is displayed, **Then** the thumbnails stay fully visible, undistorted, and do not push the publication name, publisher, or store link out of the card.

---

### User Story 2 - Reach the Publication Through Its Thumbnail (Priority: P2)

A visitor who recognizes a book by its title page clicks or taps the thumbnail and lands on the publication's detail page in the codex, the same destination the publication name already leads to.

**Why this priority**: A cover picture that is not clickable frustrates visitors because they expect it to behave like the title. It adds convenience but the feature delivers value without it.

**Independent Test**: On the homepage, activate a publication thumbnail with mouse, touch, and keyboard and verify that each leads to the same publication detail page as the publication name.

**Acceptance Scenarios**:

1. **Given** a publication card with a thumbnail, **When** the visitor activates the thumbnail, **Then** the publication's detail page opens, identical to the target of the publication name.
2. **Given** a visitor navigating with the keyboard or a screen reader, **When** they move through a publication card, **Then** the thumbnail is announced with a text alternative naming the publication, and the card does not force them through two identically announced links to the same target.

---

### User Story 3 - A Tidy Homepage When a Thumbnail Is Missing (Priority: P3)

A visitor opens the homepage while a publication has no DriveThruRPG product, or while DriveThruRPG does not deliver a thumbnail (image removed, service unreachable, slow connection). The card shows a neutral placeholder of the same size instead of a broken-image symbol or an empty gap, and the rest of the homepage works as usual.

**Why this priority**: Thumbnails come from an external source the codex does not control. Failures must not degrade the homepage, but they are the exception, not the main flow.

**Independent Test**: View the homepage with one publication that has no DriveThruRPG product and one whose thumbnail cannot be retrieved; verify that both show the placeholder, that the card layout matches cards with thumbnails, and that all other homepage content remains usable.

**Acceptance Scenarios**:

1. **Given** a listed publication without a linked DriveThruRPG product, **When** the visitor views the homepage, **Then** its card shows a neutral placeholder in the thumbnail area and no attempt is made to show a nonexistent picture.
2. **Given** a listed publication whose thumbnail cannot be retrieved, **When** the visitor views the homepage, **Then** its card shows the same placeholder instead of a broken-image indicator, and name, publisher, and links on the card remain usable.
3. **Given** DriveThruRPG responds slowly or not at all, **When** the visitor opens the homepage, **Then** the homepage text, categories, and publication names are readable and usable without waiting for the thumbnails.
4. **Given** JavaScript is enabled and a publication's JPEG thumbnail fails but its PNG thumbnail is available, **When** the card loads, **Then** the browser tries the PNG once and displays it.
5. **Given** both thumbnail formats fail, **When** the browser has attempted JPEG and PNG once each, **Then** the placeholder remains and the application initiates no further image-source attempts.
6. **Given** a JPEG has already succeeded or failed from cache before the reader script initializes, **When** initialization runs, **Then** the card displays the successful image or makes its single PNG fallback attempt, respectively.

---

### Edge Cases

- A publication is linked to several DriveThruRPG products (for example language editions): the thumbnail of its primary product is shown, so the same publication always shows the same title page.
- DriveThruRPG delivers a thumbnail with an unusual aspect ratio (landscape map pack, square card deck): the picture is fitted into the uniform thumbnail area without distortion.
- Many publications are listed at once: thumbnails outside the visible area must not delay the display of the visible part of the homepage.
- The thumbnail arrives after the rest of the page: the card must not jump or change size when the picture appears.
- No publications are listed on the homepage for the current visitor: the homepage behaves as it does today and shows no empty thumbnail area.
- A visitor uses a browser that blocks third-party images: they see the placeholder and an otherwise complete homepage.
- A visitor has JavaScript disabled: cards show the placeholder instead of the thumbnail; names, links and all other homepage content still work.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: With JavaScript enabled, the homepage MUST show a title-page thumbnail on every listed publication card whose linked DriveThruRPG product has an available JPEG or PNG 140-pixel thumbnail. Unavailable thumbnails and disabled JavaScript MUST show the documented placeholder instead.
- **FR-002**: Each card MUST use DriveThruRPG's 140-pixel thumbnail (`thumb140`) for the publication's primary product. If it is available as neither JPEG nor PNG (see FR-014), show the placeholder; do not substitute a smaller thumbnail, full-size cover, scan, or upload.
- **FR-003**: Thumbnails MUST be displayed in one uniform size and aspect ratio across all publication cards, without stretching or cropping away the title.
- **FR-004**: Each thumbnail MUST carry a text alternative that names the publication.
- **FR-005**: Activating a thumbnail MUST open the same publication detail page as the publication name on that card.
- **FR-006**: A publication without a linked DriveThruRPG product MUST show a neutral placeholder in the thumbnail area.
- **FR-007**: When a thumbnail cannot be retrieved, the card MUST show the same placeholder instead of a broken-image indicator.
- **FR-008**: Thumbnail visibility MUST NOT depend on sign-in state or ownership of the publication. The availability and disabled-JavaScript exceptions in FR-001 MUST apply equally to all visitors who see the card.
- **FR-009**: The thumbnail MUST NOT change which publications are listed, their order, or the existing content of the cards (name, publisher, store link).
- **FR-010**: Slow or failing thumbnail retrieval MUST NOT delay or prevent the display and use of the remaining homepage content.
- **FR-011**: The space for the thumbnail MUST be reserved before the picture arrives so that the page layout does not shift while thumbnails load.
- **FR-012**: Showing thumbnails MUST NOT reveal any protected codex content; only the publicly available title-page picture is shown.
- **FR-013**: Use the publication's primary DriveThruRPG product ID and existing DriveThruRPG image link to form its thumbnail link. No per-publication thumbnail list is required. Only the visitor's browser may request the thumbnail; the build, application startup and homepage backend must neither fetch it nor check its availability.
- **FR-014**: DriveThruRPG stores a product's thumbnail as either JPEG or PNG, and the publication data does not say which. With JavaScript enabled, a card with thumbnail links MUST use JPEG first, because JPEG is the majority format (52 of the 69 catalog thumbnails on 2026-11-18, against 17 PNG); if that fails, the browser MUST try PNG exactly once. When both fail, the placeholder remains. Allow at most two application-controlled image-source attempts per card: one JPEG and one PNG. HTTP redirect hops do not count as additional attempts, and the application MUST NOT retry either format.

### Key Entities

- **Publication**: A published Torg Eternity product known to the codex, with a name, a publisher, and optionally one primary DriveThruRPG product plus further related products.
- **Title-Page Thumbnail**: The small title-page picture DriveThruRPG offers for a product. It belongs to the publication's primary product and is displayed, not owned or altered, by the codex.
- **Publication Card**: The homepage element representing one publication, combining thumbnail (or placeholder), name, publisher, and store link.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With JavaScript enabled, 100% of listed publications with an available JPEG or PNG 140-pixel thumbnail display it. Unavailable images and disabled JavaScript produce the documented placeholder.
- **SC-002**: 0 broken-image indicators appear on the homepage, including when thumbnails are unavailable.
- **SC-003**: While thumbnail requests remain pending or are blocked, the homepage text, category navigation, publication links and store links remain visible and usable without waiting for image loading.
- **SC-004**: Publication cards keep an even grid: all thumbnail areas have identical dimensions on a given screen size, and no visible layout shift occurs while thumbnails load.
- **SC-005**: A visitor can identify a specific known publication on the homepage in under 5 seconds by its title page, without reading the titles of the other cards.
- **SC-006**: Every thumbnail is reachable and understandable with keyboard and screen reader (text alternative present on 100% of thumbnails).

## Assumptions

- "Publications on the homepage" means the publication cards the homepage already shows (currently the "Available Sourcebooks" section). The feature adds the thumbnail to these cards; it does not add new publication listings, for example a list of owned publications. Should the homepage list further publications later, the same thumbnail rules apply to them.
- Publication pictures elsewhere (publication overview, detail pages, purchase teaser) are out of scope.
- The title page of a publication is the cover picture of its primary DriveThruRPG product; DriveThruRPG is the only source of these pictures.
- The pictures are delivered to the visitor by DriveThruRPG, the original service. Viewing the homepage therefore makes the visitor's browser request them from DriveThruRPG without a click, which makes the visitor's IP address visible to DriveThruRPG. The owner accepts this (decided 2026-11-18); no consent step and no image proxy are introduced.
- Title-page pictures of DriveThruRPG products are public marketing material and may be displayed to all visitors, including those who do not own the publication.
- Use a thumbnail-sized link directly rather than contacting DriveThruRPG to discover available sizes.
- For a publication whose thumbnail is a PNG, the visitor's browser makes one failed JPEG source attempt before the PNG loads; either attempt may follow HTTP redirects. This extra attempt is accepted in exchange for storing no image format per publication.
- The codex does not need to know in advance whether DriveThruRPG can deliver a picture; a missing picture is handled when it fails to appear.
