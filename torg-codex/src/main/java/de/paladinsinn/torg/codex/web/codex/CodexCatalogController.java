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

package de.paladinsinn.torg.codex.web.codex;

import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseQuery;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogDetailResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria.SortDirection;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogCategoryDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.CatalogCategoryRegistry;
import de.paladinsinn.torg.codex.domain.catalog.FilterFacetDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import de.paladinsinn.torg.codex.web.codex.mapper.CodexViewMapper;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Generic inbound reader UI controller serving all 17 catalog category tabular views and htmx
 * table updates (FR-002–FR-005, FR-016, ADR-021, ADR-022).
 */
@Controller
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CodexCatalogController {

    private static final String CATALOG_RESULTS_TARGET = "catalog-results";

    private final CatalogBrowseQuery browseQuery;
    private final CodexViewMapper viewMapper;

    @Timed(value = "torg.codex.web.page", extraTags = {"page", "category"})
    @GetMapping("/{category:^(?!api$|admin$|login$|logout$|library$|notifications$|js$|webjars$|themes$|actuator$|static$|error$)[a-zA-Z0-9-]+$}")
    public String browseCategory(
            @PathVariable("category") final String categorySegment,
            @RequestParam final MultiValueMap<String, String> queryParams,
            final Authentication authentication,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Model model) {
        Optional<CatalogArea> resolvedArea = CatalogArea.fromPathSegment(categorySegment);
        if (resolvedArea.isEmpty()) {
            log.atInfo()
                    .addKeyValue("outcome", "not-found")
                    .log("Unknown catalog category path requested");
            return renderNotFound(null, authentication, response, model,
                    "The requested codex category could not be found.");
        }

        CatalogArea area = resolvedArea.get();
        if (area.isAliasPath(categorySegment)) {
            String redirectTarget = UriComponentsBuilder.fromPath(area.canonicalPath())
                    .queryParams(queryParams)
                    .encode()
                    .toUriString();
            return "redirect:" + redirectTarget;
        }

        CatalogFilterCriteria criteria = parseCriteria(area, queryParams);
        log.atDebug()
                .addKeyValue("catalogArea", area.slug())
                .addKeyValue("sortKey", criteria.sortKey())
                .log("Browsing catalog category");

        CatalogBrowseResult result = browseQuery.browse(area, criteria);
        model.addAttribute("model", viewMapper.toTableViewModel(result, authentication));

        response.setHeader(HttpHeaders.VARY, "HX-Request");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");

        if (isPartialResultsRequest(request)) {
            //noinspection SpringMVCViewInspection
            return "codex/fragments/categoryResults";
        }
        //noinspection SpringMVCViewInspection
        return "codex/pages/category";
    }

    @Timed(value = "torg.codex.web.page", extraTags = {"page", "detail"})
    @GetMapping("/{category:^(?!api$|admin$|login$|logout$|library$|notifications$|js$|webjars$|themes$|actuator$|static$|error$)[a-zA-Z0-9-]+$}/{identifier}")
    public String viewEntryDetail(
            @PathVariable("category") final String categorySegment,
            @PathVariable("identifier") final String identifier,
            final Authentication authentication,
            final HttpServletResponse response,
            final Model model) {
        Optional<CatalogArea> resolvedArea = CatalogArea.fromPathSegment(categorySegment);
        if (resolvedArea.isEmpty()) {
            return renderNotFound(null, authentication, response, model,
                    "The requested codex category could not be found.");
        }

        CatalogArea area = resolvedArea.get();
        if (area.isAliasPath(categorySegment)) {
            return "redirect:" + area.detailPath(identifier);
        }

        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        CatalogDetailResult result = browseQuery.findDetail(area, identifier);

        return switch (result) {
            case CatalogDetailResult.GrantedProductEntry granted -> {
                model.addAttribute("model", viewMapper.toDetailViewModel(granted, authentication));
                yield "codex/pages/detail";
            }
            case CatalogDetailResult.PublicReferenceEntry pubRef -> {
                model.addAttribute("model", viewMapper.toDetailViewModel(pubRef, authentication));
                yield "codex/pages/detail";
            }
            case CatalogDetailResult.RestrictedTeaser teaser -> {
                log.atInfo()
                        .addKeyValue("catalogArea", area.slug())
                        .addKeyValue("entrySlug", teaser.teaser().name())
                        .addKeyValue("outcome", "teaser")
                        .log("Restricted codex entry teaser served");
                response.setStatus(HttpStatus.FORBIDDEN.value());
                model.addAttribute("model", viewMapper.toTeaserViewModel(teaser, authentication));
                yield "codex/pages/teaser";
            }
            case CatalogDetailResult.Absent ignored -> {
                log.atInfo()
                        .addKeyValue("catalogArea", area.slug())
                        .addKeyValue("outcome", "not-found")
                        .log("Catalog entry not found");
                yield renderNotFound(area, authentication, response, model,
                        "The requested " + area.singularName().toLowerCase() + " could not be found.");
            }
        };
    }

    private String renderNotFound(
            final CatalogArea activeArea,
            final Authentication authentication,
            final HttpServletResponse response,
            final Model model,
            final String message) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        model.addAttribute("model", viewMapper.toNotFoundViewModel(
                activeArea, "Entry Not Found", message, authentication));
        return "codex/pages/notFound";
    }

    private boolean isPartialResultsRequest(final HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"))
                && CATALOG_RESULTS_TARGET.equals(request.getHeader("HX-Target"));
    }

    private CatalogFilterCriteria parseCriteria(
            final CatalogArea area,
            final MultiValueMap<String, String> params) {
        String q = firstOrEmpty(params, "q");
        Set<String> cosms = nonBlankValues(params.get("cosm"));

        CatalogCategoryDescriptor<?> descriptor = CatalogCategoryRegistry.forArea(area);
        Map<GroupingDimension, Set<String>> groupings = new EnumMap<>(GroupingDimension.class);
        for (FilterFacetDescriptor facet : descriptor.facets()) {
            if (!facet.cosmFacet() && facet.dimension() != null) {
                GroupingDimension dim = facet.dimension();
                Set<String> values = nonBlankValues(params.get(dim.getQueryParam()));
                if (!values.isEmpty()) {
                    groupings.put(dim, values);
                }
            }
        }

        String sort = firstOrEmpty(params, "sort");
        SortDirection dir = SortDirection.fromParam(firstOrEmpty(params, "dir"));
        return new CatalogFilterCriteria(q, cosms, groupings, sort, dir);
    }

    private static String firstOrEmpty(final MultiValueMap<String, String> params, final String key) {
        String value = params.getFirst(key);
        return value == null ? "" : value;
    }

    private static Set<String> nonBlankValues(final List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<String> cleaned = new LinkedHashSet<>();
        for (String item : raw) {
            if (item != null && !item.isBlank()) {
                cleaned.add(item.trim());
            }
        }
        return cleaned.isEmpty() ? Set.of() : Set.copyOf(cleaned);
    }
}
