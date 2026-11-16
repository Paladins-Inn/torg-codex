/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.torg.codex.web.codex.model;

import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.NavigationSection;
import de.paladinsinn.torg.codex.web.notification.CsrfViewModel;
import de.paladinsinn.torg.codex.web.notification.ToastRegionViewModel;

import java.util.Arrays;
import java.util.List;

/**
 * Shared global navigation context rendered in {@code codex/layout/navbar.jte} and
 * {@code codex/layout/page.jte} (FR-002, FR-005, FR-014, FR-019).
 *
 * @param activeArea    currently active catalog area, or {@code null} on home/auth pages
 * @param sections      ordered navigation dropdown groups covering all 17 categories
 * @param signedIn      whether the current session represents a signed-in DriveThruRPG user
 * @param authStatusTag short status label shown next to the Sign In / Sign Out link
 * @param csrf          optional resolved CSRF metadata when state-changing controls are rendered
 * @param toastRegion   SSR toast region and SSE stream configuration
 */
public record NavContextViewModel(
        CatalogArea activeArea,
        List<NavSectionGroup> sections,
        boolean signedIn,
        String authStatusTag,
        CsrfViewModel csrf,
        ToastRegionViewModel toastRegion) {

    /**
     * Backward-compatible constructor without resolved CSRF or toast region metadata.
     *
     * @param activeArea    currently active catalog area
     * @param sections      navigation dropdown groups
     * @param signedIn      signed-in flag
     * @param authStatusTag status badge text
     */
    public NavContextViewModel(
            final CatalogArea activeArea,
            final List<NavSectionGroup> sections,
            final boolean signedIn,
            final String authStatusTag) {
        this(activeArea, sections, signedIn, authStatusTag, null, ToastRegionViewModel.empty());
    }

    /**
     * Backward-compatible constructor without explicit toast region metadata.
     *
     * @param activeArea    currently active catalog area
     * @param sections      navigation dropdown groups
     * @param signedIn      signed-in flag
     * @param authStatusTag status badge text
     * @param csrf          CSRF view model
     */
    public NavContextViewModel(
            final CatalogArea activeArea,
            final List<NavSectionGroup> sections,
            final boolean signedIn,
            final String authStatusTag,
            final CsrfViewModel csrf) {
        this(activeArea, sections, signedIn, authStatusTag, csrf, ToastRegionViewModel.empty());
    }

    /**
     * Navigation dropdown section with its ordered catalog categories.
     *
     * @param section navigation group
     * @param areas   ordered categories in this group
     */
    public record NavSectionGroup(NavigationSection section, List<CatalogArea> areas) {
    }

    public static NavContextViewModel create(final CatalogArea activeArea, final boolean signedIn) {
        List<NavSectionGroup> groups = Arrays.stream(NavigationSection.values())
                .map(sec -> new NavSectionGroup(sec, CatalogArea.bySection(sec)))
                .toList();
        String badge = signedIn ? "DriveThruRPG Connected" : "Free Tier (Core Rules)";
        return new NavContextViewModel(activeArea, groups, signedIn, badge, null, ToastRegionViewModel.empty());
    }

    public NavContextViewModel withCsrf(final CsrfViewModel resolvedCsrf) {
        return new NavContextViewModel(activeArea, sections, signedIn, authStatusTag, resolvedCsrf, toastRegion);
    }

    public NavContextViewModel withToastRegion(final ToastRegionViewModel resolvedRegion) {
        return new NavContextViewModel(
                activeArea,
                sections,
                signedIn,
                authStatusTag,
                csrf,
                resolvedRegion == null ? ToastRegionViewModel.empty() : resolvedRegion);
    }

    public boolean isSectionActive(final NavigationSection section) {
        return activeArea != null && activeArea.section() == section;
    }
}
