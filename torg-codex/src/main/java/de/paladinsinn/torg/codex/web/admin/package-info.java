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

/**
 * Reserved inbound web-adapter surface for the future Torg Codex administrative
 * catalog-maintenance interface ({@code /admin/**}, FR-019, ADR-020).
 *
 * <p>Strictly isolated from {@code de.paladinsinn.torg.codex.web.codex}: uses its own
 * JTE layout shell ({@code admin/layout/page.jte}), stylesheet ({@code /themes/admin/admin.css}),
 * and future OIDC/RBAC security chain (ADR-007). Enforced by {@code WebUiSurfaceSeparationArchitectureTest}.
 */
package de.paladinsinn.torg.codex.web.admin;
