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

import java.util.Arrays;
import java.util.List;

/**
 * Shared global navigation context rendered in {@code codex/layout/navbar.jte} (FR-002, FR-014).
 *
 * @param activeArea    currently active catalog area, or {@code null} on home/auth pages
 * @param sections      ordered navigation dropdown groups covering all 17 categories
 * @param signedIn      whether the current session represents a signed-in DriveThruRPG user
 * @param authStatusTag short status label shown next to the Sign In / Sign Out link
 */
public record NavContextViewModel(
        CatalogArea activeArea,
        List<NavSectionGroup> sections,
        boolean signedIn,
        String authStatusTag) {

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
        return new NavContextViewModel(activeArea, groups, signedIn, badge);
    }

    public boolean isSectionActive(final NavigationSection section) {
        return activeArea != null && activeArea.section() == section;
    }
}
