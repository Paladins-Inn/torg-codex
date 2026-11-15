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

/**
 * View model for a single unlocking publication in a restricted entry's purchase teaser (FR-011).
 *
 * @param codexId         assigned product codex id
 * @param displayName     publication title (or humanized codex id fallback)
 * @param publicationPath codex publication detail URL (nullable when no publication row exists)
 * @param coverUrl        DriveThruRPG cover image URL (nullable)
 * @param storeUrl        DriveThruRPG product purchase URL (nullable)
 */
public record RequiredProductViewModel(
        String codexId,
        String displayName,
        String publicationPath,
        String coverUrl,
        String storeUrl) {
}
