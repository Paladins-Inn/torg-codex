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

package de.paladinsinn.torg.codex.web.codex.mapper;

import de.paladinsinn.security.NotLoggedInUserDetails;
import de.paladinsinn.torg.codex.api.security.CurrentUserCensorFactory;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult.FacetOption;
import de.paladinsinn.torg.codex.application.port.in.CatalogBrowseResult.ResolvedEntry;
import de.paladinsinn.torg.codex.application.port.in.CatalogDetailResult;
import de.paladinsinn.torg.codex.application.port.in.CatalogDetailResult.LinkedEntryRef;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria;
import de.paladinsinn.torg.codex.application.port.in.CatalogFilterCriteria.SortDirection;
import de.paladinsinn.torg.codex.application.port.in.CodexHomeOverview;
import de.paladinsinn.torg.codex.domain.catalog.CatalogArea;
import de.paladinsinn.torg.codex.domain.catalog.CatalogCategoryDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.ColumnDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.ColumnDescriptor.CellKind;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.AttributeField;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.Attributes;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.Prose;
import de.paladinsinn.torg.codex.domain.catalog.DetailSectionDescriptor.ProseMap;
import de.paladinsinn.torg.codex.domain.catalog.FilterFacetDescriptor;
import de.paladinsinn.torg.codex.domain.catalog.GroupingDimension;
import de.paladinsinn.torg.codex.domain.markup.Censor;
import de.paladinsinn.torg.codex.domain.markup.EntityType;
import de.paladinsinn.torg.codex.domain.model.CatalogEntry;
import de.paladinsinn.torg.codex.domain.model.ClearanceLevel;
import de.paladinsinn.torg.codex.domain.model.Publication;
import de.paladinsinn.torg.codex.web.codex.model.AttributeGroupViewModel;
import de.paladinsinn.torg.codex.web.codex.model.AttributeGroupViewModel.AttributeItem;
import de.paladinsinn.torg.codex.web.codex.model.CosmBadgeViewModel;
import de.paladinsinn.torg.codex.web.codex.model.DetailViewModel;
import de.paladinsinn.torg.codex.web.codex.model.DetailViewModel.DetailHeader;
import de.paladinsinn.torg.codex.web.codex.model.FilterPanelViewModel;
import de.paladinsinn.torg.codex.web.codex.model.FilterPanelViewModel.GroupingFacetGroup;
import de.paladinsinn.torg.codex.web.codex.model.HomeViewModel;
import de.paladinsinn.torg.codex.web.codex.model.HomeViewModel.CategoryCard;
import de.paladinsinn.torg.codex.web.codex.model.HomeViewModel.PublicationCard;
import de.paladinsinn.torg.codex.web.codex.model.LinkedEntryViewModel;
import de.paladinsinn.torg.codex.web.codex.model.NavContextViewModel;
import de.paladinsinn.torg.codex.web.codex.model.NotFoundViewModel;
import de.paladinsinn.torg.codex.web.codex.model.PublicationLinkViewModel;
import de.paladinsinn.torg.codex.web.codex.model.RenderedProseSection;
import de.paladinsinn.torg.codex.web.codex.model.RequiredProductViewModel;
import de.paladinsinn.torg.codex.web.codex.model.SanitizedHtml;
import de.paladinsinn.torg.codex.web.codex.model.TableCellViewModel;
import de.paladinsinn.torg.codex.web.codex.model.TableRowViewModel;
import de.paladinsinn.torg.codex.web.codex.model.TableViewModel;
import de.paladinsinn.torg.codex.web.codex.model.TableViewModel.ColumnHeaderViewModel;
import de.paladinsinn.torg.codex.web.codex.model.TeaserViewModel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import de.paladinsinn.torg.codex.web.notification.CsrfViewModel;
import de.paladinsinn.torg.codex.web.notification.WebNotificationSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps generic application browse/overview/detail results and category metadata descriptors into
 * reader UI view models (FR-016, W3).
 *
 * <p>Never accesses {@code ProductOwnershipResolver} or {@code GrantedAuthority} directly;
 * prose censoring is obtained exclusively through {@link CurrentUserCensorFactory}.
 */
@Component
@RequiredArgsConstructor
public class CodexViewMapper {

    private final CurrentUserCensorFactory censorFactory;

    @Setter(onMethod_ = @Autowired(required = false))
    private WebNotificationSupport notificationSupport;

    public boolean isSignedIn(final Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !(authentication.getPrincipal() instanceof NotLoggedInUserDetails);
    }

    public NavContextViewModel toNavContext(final CatalogArea activeArea, final Authentication authentication) {
        boolean signedIn = isSignedIn(authentication);
        NavContextViewModel nav = NavContextViewModel.create(activeArea, signedIn);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            var request = attrs.getRequest();
            if (signedIn) {
                nav = nav.withCsrf(CsrfViewModel.fromRequest(request));
            }
            if (notificationSupport != null) {
                nav = nav.withToastRegion(notificationSupport.buildReaderToastRegion(request, signedIn));
            }
        }
        return nav;
    }

    public HomeViewModel toHomeViewModel(final CodexHomeOverview overview, final Authentication authentication) {
        NavContextViewModel nav = toNavContext(null, authentication);
        List<CategoryCard> cards = overview.categories().stream()
                .map(c -> new CategoryCard(c.area(), c.accessibleCount()))
                .toList();
        List<PublicationCard> owned = overview.ownedPublications().stream()
                .map(this::toPublicationCard)
                .toList();
        List<PublicationCard> missing = overview.missingPublications().stream()
                .map(this::toPublicationCard)
                .toList();
        int roundedPercent = (int) Math.round(overview.unlockedPercentage());
        return new HomeViewModel(nav, cards, owned, missing, roundedPercent);
    }

    @SuppressWarnings("unchecked")
    public TableViewModel toTableViewModel(final CatalogBrowseResult result, final Authentication authentication) {
        CatalogArea area = result.area();
        CatalogFilterCriteria criteria = result.appliedCriteria();
        CatalogCategoryDescriptor<CatalogEntry> descriptor =
                (CatalogCategoryDescriptor<CatalogEntry>) result.descriptor();

        Map<String, String> cosmLabels = new LinkedHashMap<>();
        for (FacetOption cosmOpt : result.availableCosms()) {
            cosmLabels.put(cosmOpt.key(), cosmOpt.label());
        }

        List<GroupingFacetGroup> groupingGroups = new ArrayList<>();
        for (FilterFacetDescriptor facet : descriptor.facets()) {
            if (!facet.cosmFacet() && facet.dimension() != null) {
                GroupingDimension dim = facet.dimension();
                List<FacetOption> opts = result.availableFacets().getOrDefault(dim, List.of());
                if (!opts.isEmpty()) {
                    groupingGroups.add(new GroupingFacetGroup(dim, dim.getQueryParam(), dim.getDisplayName(), opts));
                }
            }
        }

        FilterPanelViewModel filterPanel = new FilterPanelViewModel(
                area.canonicalPath(),
                criteria.query(),
                result.availableCosms(),
                List.copyOf(groupingGroups),
                criteria.sortKey(),
                criteria.direction().paramValue(),
                criteria.hasActiveFilters());

        List<ColumnHeaderViewModel> headers = descriptor.columns().stream()
                .map(col -> toColumnHeader(area, col, criteria))
                .toList();

        Censor censor = censorFactory.create();
        List<TableRowViewModel> rows = result.entries().stream()
                .map(resolved -> toTableRow(area, descriptor, resolved, cosmLabels, censor))
                .toList();

        return new TableViewModel(
                toNavContext(area, authentication),
                area,
                filterPanel,
                headers,
                rows,
                rows.size(),
                result.totalAccessibleInCategory());
    }

    public DetailViewModel toDetailViewModel(
            final CatalogDetailResult.GrantedProductEntry granted,
            final Authentication authentication) {
        List<PublicationLinkViewModel> pubLinks = mapPublications(
                granted.entry().productCodexIds(), granted.publications());
        return buildDetailViewModel(
                toNavContext(granted.area(), authentication),
                granted.descriptor(),
                granted.entry(),
                granted.canonicalSlug(),
                granted.cosmLabels(),
                granted.embeddedReferences(),
                pubLinks);
    }

    public DetailViewModel toDetailViewModel(
            final CatalogDetailResult.PublicReferenceEntry pubRef,
            final Authentication authentication) {
        return buildDetailViewModel(
                toNavContext(pubRef.area(), authentication),
                pubRef.descriptor(),
                pubRef.entry(),
                pubRef.canonicalSlug(),
                Map.of(),
                pubRef.embeddedReferences(),
                List.of());
    }

    public TeaserViewModel toTeaserViewModel(
            final CatalogDetailResult.RestrictedTeaser teaserResult,
            final Authentication authentication) {
        List<RequiredProductViewModel> products = teaserResult.requiredProducts().stream()
                .map(req -> req.publication()
                        .map(pub -> new RequiredProductViewModel(
                                req.codexId(),
                                pub.name(),
                                CatalogArea.PUBLICATION.detailPath(pub.slug()),
                                pub.primaryProductId() > 0 ? pub.coverURL() : null,
                                pub.productUrl()))
                        .orElseGet(() -> new RequiredProductViewModel(
                                req.codexId(),
                                EntityType.toDisplayName(req.codexId()),
                                null,
                                null,
                                null)))
                .toList();
        return new TeaserViewModel(
                toNavContext(teaserResult.area(), authentication),
                teaserResult.area(),
                teaserResult.teaser().name(),
                products);
    }

    public NotFoundViewModel toNotFoundViewModel(
            final CatalogArea activeArea,
            final String title,
            final String message,
            final Authentication authentication) {
        return new NotFoundViewModel(toNavContext(activeArea, authentication), title, message);
    }

    @SuppressWarnings("unchecked")
    private DetailViewModel buildDetailViewModel(
            final NavContextViewModel nav,
            final CatalogCategoryDescriptor<? extends CatalogEntry> rawDescriptor,
            final CatalogEntry entry,
            final String canonicalSlug,
            final Map<String, String> cosmLabels,
            final Map<String, List<LinkedEntryRef>> embeddedReferences,
            final List<PublicationLinkViewModel> publications) {
        CatalogCategoryDescriptor<CatalogEntry> descriptor =
                (CatalogCategoryDescriptor<CatalogEntry>) rawDescriptor;
        CatalogArea area = descriptor.area();
        Censor censor = censorFactory.create();

        String clearanceSymbol = entry.clearance().map(ClearanceLevel::getSymbol).orElse(null);
        String clearanceName = entry.clearance()
                .map(c -> EntityType.toDisplayName(c.getFullName()))
                .orElse(null);
        List<CosmBadgeViewModel> cosms = entry.cosmSlugs().stream()
                .sorted()
                .map(slug -> CosmBadgeViewModel.of(slug, cosmLabels.get(slug)))
                .toList();

        DetailHeader header = new DetailHeader(
                entry.name(),
                area.detailPath(canonicalSlug),
                clearanceSymbol,
                clearanceName,
                cosms);

        List<AttributeGroupViewModel> attributeGroups = new ArrayList<>();
        List<RenderedProseSection> proseSections = new ArrayList<>();

        for (DetailSectionDescriptor<CatalogEntry> section : descriptor.detailSections()) {
            switch (section) {
                case Attributes<CatalogEntry> attrs -> {
                    List<AttributeItem> items = new ArrayList<>();
                    for (AttributeField<CatalogEntry> field : attrs.fields()) {
                        String raw = field.valueExtractor().apply(entry);
                        if (raw != null && !raw.isBlank()) {
                            if (field.renderAsMarkup()) {
                                SanitizedHtml rendered = new SanitizedHtml(censor.apply(raw));
                                if (!rendered.isEmpty()) {
                                    items.add(new AttributeItem(field.label(), "", rendered));
                                }
                            } else {
                                items.add(new AttributeItem(field.label(), raw.trim(), null));
                            }
                        }
                    }
                    if (!items.isEmpty()) {
                        attributeGroups.add(new AttributeGroupViewModel(attrs.title(), List.copyOf(items)));
                    }
                }
                case Prose<CatalogEntry> prose -> {
                    String raw = prose.rawMarkupExtractor().apply(entry);
                    if (raw != null && !raw.isBlank()) {
                        SanitizedHtml rendered = new SanitizedHtml(censor.apply(raw));
                        if (!rendered.isEmpty()) {
                            proseSections.add(new RenderedProseSection(prose.title(), rendered));
                        }
                    }
                }
                case ProseMap<CatalogEntry> proseMap -> {
                    Map<String, String> entries = proseMap.rawMarkupMapExtractor().apply(entry);
                    if (entries != null && !entries.isEmpty()) {
                        for (Map.Entry<String, String> item : entries.entrySet()) {
                            if (item.getValue() != null && !item.getValue().isBlank()) {
                                SanitizedHtml rendered = new SanitizedHtml(censor.apply(item.getValue()));
                                if (!rendered.isEmpty()) {
                                    proseSections.add(new RenderedProseSection(
                                            proseMap.title() + ": " + item.getKey(), rendered));
                                }
                            }
                        }
                    }
                }
                case DetailSectionDescriptor.EmbeddedReferences<CatalogEntry> ignored -> {
                    // Rendered from resolved embeddedReferences map.
                }
            }
        }

        Map<String, List<LinkedEntryViewModel>> embeddedGroups = new LinkedHashMap<>();
        embeddedReferences.forEach((title, refs) -> {
            List<LinkedEntryViewModel> links = refs.stream()
                    .map(r -> new LinkedEntryViewModel(r.name(), r.detailPath(), r.targetArea()))
                    .toList();
            if (!links.isEmpty()) {
                embeddedGroups.put(title, links);
            }
        });

        return new DetailViewModel(
                nav,
                area,
                header,
                List.copyOf(attributeGroups),
                List.copyOf(proseSections),
                Map.copyOf(embeddedGroups),
                publications);
    }

    private List<PublicationLinkViewModel> mapPublications(
            final Set<String> codexIds,
            final List<Publication> resolvedPublications) {
        Map<String, Publication> byCodexId = new LinkedHashMap<>();
        for (Publication pub : resolvedPublications) {
            byCodexId.put(pub.codexId(), pub);
        }
        return codexIds.stream()
                .sorted()
                .map(codexId -> {
                    Publication pub = byCodexId.get(codexId);
                    if (pub != null) {
                        return new PublicationLinkViewModel(
                                codexId,
                                pub.name(),
                                CatalogArea.PUBLICATION.detailPath(pub.slug()),
                                pub.productUrl());
                    }
                    return new PublicationLinkViewModel(
                            codexId,
                            EntityType.toDisplayName(codexId),
                            null,
                            null);
                })
                .toList();
    }

    private PublicationCard toPublicationCard(final Publication pub) {
        String publisher = pub.thirdParty() == null || pub.thirdParty().isBlank()
                ? "Ulisses Spiele"
                : pub.thirdParty();
        String cover = pub.primaryProductId() > 0 ? pub.coverURL() : null;
        return new PublicationCard(
                pub.codexId(),
                pub.name(),
                CatalogArea.PUBLICATION.detailPath(pub.slug()),
                publisher,
                cover,
                pub.productUrl());
    }

    private ColumnHeaderViewModel toColumnHeader(
            final CatalogArea area,
            final ColumnDescriptor<CatalogEntry> col,
            final CatalogFilterCriteria criteria) {
        if (!col.sortable()) {
            return new ColumnHeaderViewModel(col.key(), col.header(), false, false, true, "");
        }
        boolean active = col.key().equalsIgnoreCase(criteria.sortKey());
        SortDirection nextDir = active ? criteria.direction().toggled() : SortDirection.ASC;
        String sortUrl = buildSortUrl(area.canonicalPath(), criteria, col.key(), nextDir);
        return new ColumnHeaderViewModel(
                col.key(),
                col.header(),
                true,
                active,
                criteria.direction() == SortDirection.ASC,
                sortUrl);
    }

    private String buildSortUrl(
            final String basePath,
            final CatalogFilterCriteria criteria,
            final String nextSortKey,
            final SortDirection nextDir) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        if (!criteria.query().isEmpty()) {
            builder.queryParam("q", criteria.query());
        }
        for (String cosm : criteria.cosms()) {
            builder.queryParam("cosm", cosm);
        }
        criteria.groupings().forEach((dim, vals) -> {
            for (String v : vals) {
                builder.queryParam(dim.getQueryParam(), v);
            }
        });
        builder.queryParam("sort", nextSortKey);
        builder.queryParam("dir", nextDir.paramValue());
        return builder.encode().toUriString();
    }

    private TableRowViewModel toTableRow(
            final CatalogArea area,
            final CatalogCategoryDescriptor<CatalogEntry> descriptor,
            final ResolvedEntry resolved,
            final Map<String, String> cosmLabels,
            final Censor censor) {
        String detailPath = area.detailPath(resolved.canonicalSlug());
        CatalogEntry entry = resolved.entry();
        List<TableCellViewModel> cells = descriptor.columns().stream()
                .map(col -> toCell(col, entry, detailPath, cosmLabels, censor))
                .toList();
        return new TableRowViewModel(detailPath, cells);
    }

    private TableCellViewModel toCell(
            final ColumnDescriptor<CatalogEntry> col,
            final CatalogEntry entry,
            final String detailPath,
            final Map<String, String> cosmLabels,
            final Censor censor) {
        String text = col.textExtractor().apply(entry);
        return switch (col.kind()) {
            case PRIMARY_LINK -> new TableCellViewModel(CellKind.PRIMARY_LINK, text, detailPath, List.of(), null);
            case MARKUP -> new TableCellViewModel(CellKind.MARKUP, "", "", List.of(),
                    new SanitizedHtml(censor.apply(text)));
            case EXTERNAL_LINK -> new TableCellViewModel(CellKind.EXTERNAL_LINK,
                    text.isEmpty() ? "" : "Store", text, List.of(), null);
            case COSM_BADGES -> {
                List<CosmBadgeViewModel> badges = col.listExtractor().apply(entry).stream()
                        .map(slug -> CosmBadgeViewModel.of(slug, cosmLabels.get(slug)))
                        .toList();
                yield new TableCellViewModel(CellKind.COSM_BADGES, text, "", badges, null);
            }
            default -> new TableCellViewModel(col.kind(), text, "", List.of(), null);
        };
    }
}
