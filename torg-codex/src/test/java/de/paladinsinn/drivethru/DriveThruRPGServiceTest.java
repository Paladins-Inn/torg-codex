package de.paladinsinn.drivethru;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.paladinsinn.drivethru.adapter.out.http.DriveThruRpgProductAdapter;
import de.paladinsinn.drivethru.client.DriveThruProperties;
import de.paladinsinn.drivethru.client.DriveThruRPGClientConfig;
import de.paladinsinn.drivethru.client.DriveThruRPGClient;
import de.paladinsinn.drivethru.products.OwnedProduct;
import de.paladinsinn.drivethru.products.Product;
import de.paladinsinn.drivethru.publishers.Publisher;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import de.paladinsinn.security.DriveThruUserDetails;
import de.paladinsinn.security.ApiKeyAuthenticationToken;
import de.paladinsinn.security.DriveThruAuthenticationProvider;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import de.paladinsinn.torg.codex.domain.model.CatalogProduct;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

/**
 * Verifies the DriveThruRPG vBeta OpenAPI and JSON:API wire protocol without a database.
 */
@WireMockTest
class DriveThruRPGServiceTest {
    private static final String API_KEY = "aabbccddeeff00112233445566778899aabbccdd";
    private static final String ACCESS_TOKEN = "wiremock-jwt-bearer-token-777";
    private static final String JSON_API = "application/vnd.api+json";
    private static final long VALID_REFRESH_TTL = 4102444800L;
    private static final int PAGE_SIZE = 200;
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int UNAVAILABLE = 503;

    private DriveThruRPGService service;
    private DriveThruRPGClient client;
    private SimpleMeterRegistry meterRegistry;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger serviceLogger = (Logger) LoggerFactory.getLogger(DriveThruRPGService.class);
    private final Logger providerLogger = (Logger) LoggerFactory.getLogger(DriveThruAuthenticationProvider.class);
    private Level originalServiceLevel;
    private Level originalProviderLevel;

    @BeforeEach
    void setUp(final WireMockRuntimeInfo wireMock) {
        DriveThruProperties properties = new DriveThruProperties();
        properties.setBaseUrl(wireMock.getHttpBaseUrl() + "/api/vBeta");
        meterRegistry = new SimpleMeterRegistry();
        client = new DriveThruRPGClientConfig().driveThruRPGClient(properties, meterRegistry);
        service = new DriveThruRPGService(client);
        originalServiceLevel = serviceLogger.getLevel();
        originalProviderLevel = providerLogger.getLevel();
        appender.start();
        serviceLogger.addAppender(appender);
        providerLogger.addAppender(appender);
        serviceLogger.setLevel(Level.DEBUG);
        providerLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void restoreLoggers() {
        serviceLogger.detachAppender(appender);
        providerLogger.detachAppender(appender);
        serviceLogger.setLevel(originalServiceLevel);
        providerLogger.setLevel(originalProviderLevel);
        appender.stop();
        meterRegistry.close();
    }

    @Test
    void authenticatesAndLoadsLibraryUsingTheOpenApiJsonApiContract() throws Exception {
        DrivethruToken token = service.getToken(API_KEY);

        assertThat(token.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(token.getBearerToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(token.getRefreshToken()).isEqualTo("wiremock-refresh-token");
        assertThat(token.getRefreshTokenTTL()).isEqualTo(VALID_REFRESH_TTL);
        assertThat(service.getIdsOfOwnedProducts(token)).containsExactly(216248, 284210);
        assertThat(token.getCustomerId()).isEqualTo("424242");
        assertThat(token.toString()).doesNotContain(API_KEY, ACCESS_TOKEN, "wiremock-refresh-token");
        DriveThruUserDetails user = new DriveThruUserDetails(token, List.of("sourcebook-aysle"));
        assertThat(user.getUsername()).isEqualTo("424242");
        assertThat(user.getAuthorities()).extracting(Object::toString).contains("ROLE_sourcebook-aysle");

        verify(postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key"))
                .withQueryParam("applicationKey", equalTo(API_KEY))
                .withHeader("Authorization", absent())
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson("{}")));
        verify(getRequestedFor(urlPathEqualTo("/api/vBeta/order_products"))
                .withHeader("Authorization", equalTo(ACCESS_TOKEN))
                .withHeader("Accept", containing(JSON_API))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("pageSize", equalTo("200"))
                .withQueryParam("archived", equalTo("0"))
                .withQueryParam("library", equalTo("true"))
                .withQueryParam("getChecksum", equalTo("0"))
                .withQueryParam("getFilters", equalTo("0"))
                .withQueryParam("fields", absent()));
        assertMetrics("token-exchange", "success", 1);
        assertMetrics("library-page", "success", 1);
    }

    @Test
    void readsMultiplePagesFollowingJsonApiNextLinksAndStopsWhenNextIsNull() throws Exception {
        String firstPage = orderProductCollectionJson(
                IntStream.rangeClosed(1, PAGE_SIZE).boxed().toList(),
                "/api/vBeta/order_products?page=2&pageSize=200");
        String secondPage = orderProductCollectionJson(List.of(999001), null);
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API).withBody(firstPage)));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API).withBody(secondPage)));

        List<Integer> expected = IntStream.concat(
                IntStream.rangeClosed(1, PAGE_SIZE),
                IntStream.of(999001)).boxed().toList();
        assertThat(service.getIdsOfOwnedProducts(service.getToken(API_KEY)))
                .containsExactlyElementsOf(expected);
        verify(2, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));
        assertMetrics("library-page", "success", 2);
    }

    @Test
    void respectsServerPageSizeCapsWithoutSkippingProducts() throws Exception {
        String firstPage = orderProductCollectionJson(List.of(1, 2), "/api/vBeta/order_products?page=2&pageSize=2")
                .replace("\"itemsPerPage\":200", "\"itemsPerPage\":2");
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API).withBody(firstPage)));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("2"))
                .withQueryParam("pageSize", equalTo("2"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBody(orderProductCollectionJson(List.of(3), null))));

        assertThat(service.getIdsOfOwnedProducts(service.getToken(API_KEY))).containsExactly(1, 2, 3);
        verify(2, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));
    }

    @Test
    void usesServerPageSizeWhenPaginationLinksAreMissing() throws Exception {
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBody("""
                                {"meta":{"itemsPerPage":2,"currentPage":1},"data":[
                                  {"attributes":{"productId":1}},{"attributes":{"productId":2}}]}
                                """)));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("2"))
                .withQueryParam("pageSize", equalTo("2"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBody(orderProductCollectionJson(List.of(), null))));

        assertThat(service.getIdsOfOwnedProducts(service.getToken(API_KEY))).containsExactly(1, 2);
    }

    @Test
    void stopsAfterAFullPageWhenJsonApiNextLinkIsNull() throws Exception {
        String singleFullPage = orderProductCollectionJson(
                IntStream.rangeClosed(1, PAGE_SIZE).boxed().toList(),
                null);
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API).withBody(singleFullPage)));

        assertThat(service.getIdsOfOwnedProducts(service.getToken(API_KEY))).hasSize(PAGE_SIZE);
        verify(1, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));
    }

    @Test
    void emptyLibraryFallsBackToLocalRandomSubjectUsername() throws Exception {
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBody(orderProductCollectionJson(List.of(), null))));

        DrivethruToken token = service.getToken(API_KEY);
        assertThat(service.getIdsOfOwnedProducts(token)).isEmpty();
        assertThat(token.getCustomerId()).isNull();
        DriveThruUserDetails user = new DriveThruUserDetails(token, List.of());
        assertThat(user.getUsername()).isNotBlank().isNotEqualTo(API_KEY).isNotEqualTo(ACCESS_TOKEN);
    }

    @Test
    void skipsCookieRefreshWhenRefreshTokenTtlIsAlreadyExpired() throws Exception {
        stubFor(post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .inScenario("expired-ttl").whenScenarioStateIs("Started")
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"" + ACCESS_TOKEN + "\","
                                + "\"refreshToken\":\"expired-refresh-token\","
                                + "\"refreshTokenTTL\":1}"))
                .willSetStateTo("renewed"));
        stubFor(post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .inScenario("expired-ttl").whenScenarioStateIs("renewed")
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"fresh-access-token\","
                                + "\"refreshToken\":\"fresh-refresh-token\","
                                + "\"refreshTokenTTL\":4102444800}")));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo(ACCESS_TOKEN))
                .willReturn(aResponse().withStatus(UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("fresh-access-token"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBodyFile("drivethru/owned-products-424242-response.json")));

        assertThat(service.getIdsOfOwnedProducts(service.getToken(API_KEY)))
                .containsExactly(216248, 284210);
        verify(0, postRequestedFor(urlPathEqualTo("/api/vBeta/auth/refresh")));
        verify(2, postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key")));
    }

    @Test
    void renewsViaRefreshTokenCookieOnUnauthorizedAndRetriesTheSamePage() throws Exception {
        stubFor(post(urlPathEqualTo("/api/vBeta/auth/refresh")).atPriority(1)
                .withHeader("Cookie", equalTo("refreshToken=wiremock-refresh-token"))
                .withRequestBody(equalToJson("{}"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"refreshed-access-token\","
                                + "\"refreshToken\":\"rotated-refresh-token\","
                                + "\"refreshTokenTTL\":4102444800}")));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo(ACCESS_TOKEN))
                .willReturn(aResponse().withStatus(UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("refreshed-access-token"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBodyFile("drivethru/owned-products-424242-response.json")));

        DrivethruToken token = service.getToken(API_KEY);
        assertThat(service.getIdsOfOwnedProducts(token)).containsExactly(216248, 284210);
        assertThat(token.getRefreshToken()).isEqualTo("rotated-refresh-token");
        verify(1, postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key")));
        verify(1, postRequestedFor(urlPathEqualTo("/api/vBeta/auth/refresh")));
        verify(2, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products"))
                .withQueryParam("page", equalTo("1")));
        assertMetrics("token-refresh", "success", 1);
        assertMetrics("library-page", "error", 1);
        assertMetrics("library-page", "success", 1);
    }

    @Test
    void fallsBackToApplicationKeyExchangeWhenRefreshTokenIsRejectedOnLaterPage() throws Exception {
        String firstPage = orderProductCollectionJson(
                IntStream.rangeClosed(1, PAGE_SIZE).boxed().toList(),
                "/api/vBeta/order_products?page=2&pageSize=200");
        stubFor(post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .inScenario("fallback-renewal").whenScenarioStateIs("Started")
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBodyFile("drivethru/token-valid-response.json")).willSetStateTo("expired"));
        stubFor(post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .inScenario("fallback-renewal").whenScenarioStateIs("expired")
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"reexchanged-access-token\","
                                + "\"refreshToken\":\"fresh-refresh\","
                                + "\"refreshTokenTTL\":4102444800}")));
        stubFor(post(urlPathEqualTo("/api/vBeta/auth/refresh")).atPriority(1)
                .willReturn(aResponse().withStatus(UNAUTHORIZED)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errorCode\":\"refresh_expired\","
                                + "\"message\":\"Refresh token expired.\","
                                + "\"authState\":\"refresh_expired\"}")));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API).withBody(firstPage)));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("2"))
                .inScenario("second-page").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(UNAUTHORIZED)).willSetStateTo("renewed"));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withQueryParam("page", equalTo("2"))
                .withHeader("Authorization", equalTo("reexchanged-access-token"))
                .inScenario("second-page").whenScenarioStateIs("renewed")
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBody(orderProductCollectionJson(List.of(), null))));

        assertThat(service.getIdsOfOwnedProducts(service.getToken(API_KEY))).hasSize(PAGE_SIZE);
        verify(1, postRequestedFor(urlPathEqualTo("/api/vBeta/auth/refresh")));
        verify(2, postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key")));
        verify(1, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products"))
                .withQueryParam("page", equalTo("1")));
        verify(2, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products"))
                .withQueryParam("page", equalTo("2")));
    }

    @Test
    void loadsCatalogProductAndPublisherFromJsonApiDocumentsAndMapsNotFoundToEmpty() {
        Optional<Product> product = service.getProduct("216248");
        assertThat(product).hasValueSatisfying(found -> {
            assertThat(found.getProductsId()).isEqualTo(216248);
            assertThat(found.getProductsName()).isEqualTo("Torg Eternity - Core Rules");
            assertThat(found.getPublisherId()).isEqualTo(3444);
            assertThat(found.getPublisherName()).isEqualTo("Ulisses Spiele");
            assertThat(found.getCoverURL()).isEqualTo("https://api.drivethrurpg.com/images/3444/216248.jpg");
        });

        CatalogProduct domainProduct = new DriveThruRpgProductAdapter(service)
                .findProduct("216248")
                .orElseThrow();
        assertThat(domainProduct.id()).isEqualTo(216248);
        assertThat(domainProduct.name()).isEqualTo("Torg Eternity - Core Rules");
        assertThat(domainProduct.coverUrl()).isEqualTo("https://api.drivethrurpg.com/images/3444/216248.jpg");

        Optional<Publisher> publisher = service.getPublisher("3444");
        assertThat(publisher).hasValueSatisfying(found -> {
            assertThat(found.getPublisherId()).isEqualTo(3444);
            assertThat(found.getPublisherName()).isEqualTo("Ulisses Spiele");
        });

        stubFor(get(urlPathEqualTo("/api/vBeta/products/999999"))
                .willReturn(aResponse().withStatus(NOT_FOUND)));
        stubFor(get(urlPathEqualTo("/api/vBeta/publishers/999999"))
                .willReturn(aResponse().withStatus(NOT_FOUND)));
        assertThat(service.getProduct("999999")).isEmpty();
        assertThat(service.getPublisher("999999")).isEmpty();
        assertMetrics("product", "success", 2);
        assertMetrics("product", "error", 1);
        assertMetrics("publisher", "success", 1);
        assertMetrics("publisher", "error", 1);
    }

    @Test
    void measuresTransportTimeoutsWithoutAddingCredentialsToMetricTags(final WireMockRuntimeInfo wireMock) {
        DriveThruProperties properties = new DriveThruProperties();
        properties.setBaseUrl(wireMock.getHttpBaseUrl() + "/api/vBeta");
        properties.setReadTimeout(Duration.ofMillis(50));
        DriveThruRPGService slowService = new DriveThruRPGService(
                new DriveThruRPGClientConfig().driveThruRPGClient(properties, meterRegistry));
        stubFor(post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .willReturn(aResponse().withFixedDelay(300).withBody(API_KEY)));

        assertThatThrownBy(() -> slowService.getToken(API_KEY)).isInstanceOf(AuthenticationServiceException.class);
        assertMetrics("token-exchange", "error", 1);
        assertThat(meterRegistry.getMeters()).allSatisfy(meter -> {
            assertThat(meter.getId().getTags()).extracting(tag -> tag.getKey()).containsExactly("operation", "outcome");
            assertThat(meter.getId().toString()).doesNotContain(API_KEY, ACCESS_TOKEN, "applicationKey", "http://");
        });
    }

    @Test
    void exportsPrometheusDurationAndRequestCount(final WireMockRuntimeInfo wireMock) throws Exception {
        DriveThruProperties properties = new DriveThruProperties();
        properties.setBaseUrl(wireMock.getHttpBaseUrl() + "/api/vBeta");
        PrometheusMeterRegistry prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        try {
            DriveThruRPGService measured = new DriveThruRPGService(
                    new DriveThruRPGClientConfig().driveThruRPGClient(properties, prometheus));
            measured.getToken(API_KEY);

            assertThat(prometheus.scrape())
                    .contains("drivethru_http_client_duration_seconds_count{operation=\"token-exchange\",outcome=\"success\"} 1",
                            "drivethru_http_client_duration_seconds_sum{operation=\"token-exchange\",outcome=\"success\"}",
                            "drivethru_http_client_requests_total{operation=\"token-exchange\",outcome=\"success\"} 1")
                    .doesNotContain(API_KEY, ACCESS_TOKEN, "applicationKey", "http://");
        } finally {
            prometheus.close();
        }
    }

    @Test
    void measuresIndividualOrderProductRequests() {
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products/123")).willReturn(
                aResponse().withHeader("Content-Type", JSON_API)
                        .withBody("{\"data\":{\"id\":\"123\",\"type\":\"OrderProduct\","
                                + "\"attributes\":{\"productId\":216248}}}")));

        assertThat(client.getOrderProduct(ACCESS_TOKEN, 123).getAttributes())
                .hasValueSatisfying(product -> assertThat(product.getId()).isEqualTo(216248));
        assertMetrics("order-product", "success", 1);
    }

    @Test
    void retainsTheLegacyClientMethodSignaturesAsAdapters() {
        assertThat(client.getToken("Bearer " + API_KEY).getData()).hasValueSatisfying(
                data -> assertThat(data).containsEntry("access_token", ACCESS_TOKEN));
        assertThat(client.getOwnedProducts(ACCESS_TOKEN, "unused", 1, PAGE_SIZE, 0).getData())
                .extracting(OwnedProduct::getId).containsExactly(216248, 284210);
        assertThat(client.getProduct("216248").getData())
                .map(Product::getProductsName)
                .contains("Torg Eternity - Core Rules");
        assertThat(client.getPublisher("3444").getData())
                .map(Publisher::getPublisherName)
                .contains("Ulisses Spiele");
    }

    @Test
    void repeatedUnauthorizedDoesNotLoopOrReturnPartialOwnership() throws Exception {
        stubFor(post(urlPathEqualTo("/api/vBeta/auth/refresh")).atPriority(1)
                .willReturn(aResponse().withStatus(UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .willReturn(aResponse().withStatus(UNAUTHORIZED)));
        DrivethruToken token = service.getToken(API_KEY);

        assertThatThrownBy(() -> service.getIdsOfOwnedProducts(token))
                .isInstanceOf(NoValidTokenException.class);
        verify(1, postRequestedFor(urlPathEqualTo("/api/vBeta/auth/refresh")));
        verify(2, postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key")));
        verify(2, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));
    }

    @Test
    void rejectsAnInvalidApplicationKey() {
        assertThatThrownBy(() -> service.getToken("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef"))
                .isInstanceOf(NoValidTokenException.class);
        assertMetrics("token-exchange", "error", 1);
    }

    @Test
    void reportsServiceFailuresWithoutExposingTheKeyInTheException() {
        stubFor(post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .willReturn(aResponse().withStatus(UNAVAILABLE).withBody(API_KEY)));

        assertThatThrownBy(() -> service.getToken(API_KEY))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageNotContaining(API_KEY)
                .hasNoCause();
        assertMetrics("token-exchange", "error", 1);
        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).isEqualTo("DriveThruRPG token request failed.");
            assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                    .contains(tuple("statusCode", UNAVAILABLE));
            assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key)
                    .contains("exceptionClass", "causeClass");
        });
        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("DriveThruRPG HTTP request failed.");
            assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                    .contains(tuple("statusCode", UNAVAILABLE), tuple("operation", "token-exchange"));
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    void aSuccessfulResponseWithoutATokenIsAProtocolFailure() {
        stubFor(post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{}")));

        assertThatThrownBy(() -> service.getToken(API_KEY))
                .isInstanceOf(AuthenticationServiceException.class);
        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("DriveThruRPG token response received.");
            assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                    .contains(tuple("responsePresent", true), tuple("accessTokenPresent", false));
        });
    }

    @Test
    void tokenRejectionLogsStatusAndStageWithoutCredentialsOrResponseBody() {
        String responseBody = "sensitive-upstream-body " + API_KEY + " " + ACCESS_TOKEN;
        stubFor(post(urlPathEqualTo("/api/vBeta/auth_key")).atPriority(1)
                .willReturn(aResponse().withStatus(FORBIDDEN)
                        .withHeader("Location", "/redirect?applicationKey=" + API_KEY)
                        .withBody(responseBody)));
        DriveThruAuthenticationProvider provider =
                new DriveThruAuthenticationProvider(service, mock(CatalogReferenceQuery.class));

        assertThatThrownBy(() -> provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(API_KEY)))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("DriveThruRPG HTTP request failed.");
            assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                    .contains(tuple("operation", "token-exchange"), tuple("endpoint", "/auth_key"),
                            tuple("statusCode", FORBIDDEN));
        });
        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("DriveThruRPG authentication rejected.");
            assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                    .contains(tuple("stage", "token-exchange"));
        });
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs())
                    .doesNotContain(API_KEY, ACCESS_TOKEN, "sensitive-upstream-body", "applicationKey=");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    void repeatedLibraryRejectionIsDistinguishableFromTokenExchangeFailure() {
        stubFor(post(urlPathEqualTo("/api/vBeta/auth/refresh")).atPriority(1)
                .willReturn(aResponse().withStatus(UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .willReturn(aResponse().withStatus(UNAUTHORIZED).withBody(ACCESS_TOKEN)));
        DriveThruAuthenticationProvider provider =
                new DriveThruAuthenticationProvider(service, mock(CatalogReferenceQuery.class));

        assertThatThrownBy(() -> provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(API_KEY)))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("DriveThruRPG HTTP request failed.");
            assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                    .contains(tuple("operation", "library-fetch"), tuple("endpoint", "/order_products"),
                            tuple("statusCode", UNAUTHORIZED));
        });
        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage())
                    .isEqualTo("DriveThruRPG library access still unauthorized after token renewal.");
        });
        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("DriveThruRPG authentication rejected.");
            assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                    .contains(tuple("stage", "library-load"));
        });
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs())
                    .doesNotContain(API_KEY, ACCESS_TOKEN, "wiremock-refresh-token");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    void streamsBatchesImmediatelyBeforeRequestingNextPageAndDeduplicatesSnapshot() throws Exception {
        DrivethruToken token = new DrivethruToken();
        token.setAccessToken("wiremock-streaming-bearer-token");
        List<DriveThruLibraryBatch> receivedBatches = new java.util.ArrayList<>();

        DriveThruLibrarySnapshot snapshot = service.streamOwnedProductBatches(token, batch -> {
            receivedBatches.add(batch);
            if (batch.pageNumber() == 1) {
                verify(1, getRequestedFor(urlPathEqualTo("/api/vBeta/order_products")));
                assertThat(batch.productIds()).containsExactly(216248);
                assertThat(batch.customerId()).isEqualTo("424242");
            }
        });

        assertThat(receivedBatches).hasSize(2);
        assertThat(receivedBatches.get(1).pageNumber()).isEqualTo(2);
        assertThat(receivedBatches.get(1).productIds()).containsExactly(284210, 216248);
        assertThat(snapshot.productIds()).containsExactly(216248, 284210);
        assertThat(snapshot.customerId()).isEqualTo("424242");
        assertThat(snapshot.loadedAt()).isNotNull();
        assertThatThrownBy(() -> snapshot.productIds().add(999))
                .isInstanceOf(UnsupportedOperationException.class);
        assertMetrics("library-page", "success", 2);
    }

    @Test
    void streamingHandlesEmptyLibrariesServerCapsMissingLinksAndPartialOrSupersededFailures() throws Exception {
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("empty-bearer"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBody(orderProductCollectionJson(List.of(), null))));

        DrivethruToken emptyToken = new DrivethruToken();
        emptyToken.setAccessToken("empty-bearer");
        List<DriveThruLibraryBatch> emptyBatches = new java.util.ArrayList<>();
        DriveThruLibrarySnapshot emptySnapshot = service.streamOwnedProductBatches(emptyToken, emptyBatches::add);
        assertThat(emptyBatches).isEmpty();
        assertThat(emptySnapshot.productIds()).isEmpty();
        assertThat(emptySnapshot.customerId()).isNull();

        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("partial-fail-bearer"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", JSON_API)
                        .withBody(orderProductCollectionJson(
                                List.of(216248), "/api/vBeta/order_products?page=2&pageSize=200"))));
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("partial-fail-bearer"))
                .withQueryParam("page", equalTo("2"))
                .willReturn(aResponse().withStatus(UNAVAILABLE)));

        DrivethruToken failingToken = new DrivethruToken();
        failingToken.setAccessToken("partial-fail-bearer");
        List<DriveThruLibraryBatch> partialBatches = new java.util.ArrayList<>();
        assertThatThrownBy(() -> service.streamOwnedProductBatches(failingToken, partialBatches::add))
                .isInstanceOf(AuthenticationServiceException.class);
        assertThat(partialBatches).singleElement()
                .extracting(DriveThruLibraryBatch::productIds)
                .isEqualTo(List.of(216248));

        DrivethruToken supersededToken = new DrivethruToken();
        supersededToken.setAccessToken("wiremock-streaming-bearer-token");
        assertThatThrownBy(() -> service.streamOwnedProductBatches(supersededToken, batch -> {
            throw new SupersededSyncException("superseded after batch 1");
        })).isInstanceOf(SupersededSyncException.class);
    }

    @Test
    void sessionCopyRenewalIsRefreshOnlyAndNeverReExchangesMissingApplicationKey() throws Exception {
        stubFor(get(urlPathEqualTo("/api/vBeta/order_products")).atPriority(1)
                .withHeader("Authorization", equalTo("expired-session-jwt"))
                .willReturn(aResponse().withStatus(UNAUTHORIZED)));
        stubFor(post(urlPathEqualTo("/api/vBeta/auth/refresh")).atPriority(1)
                .withHeader("Cookie", equalTo("refreshToken=rejected-refresh-token"))
                .willReturn(aResponse().withStatus(UNAUTHORIZED)));

        DrivethruToken original = service.getToken(API_KEY);
        meterRegistry.clear();

        DrivethruToken missingRefresh = original.sessionCopy();
        missingRefresh.setAccessToken("expired-session-jwt");
        missingRefresh.setRefreshToken(null);
        assertThatThrownBy(() -> service.streamOwnedProductBatches(missingRefresh, batch -> { }))
                .isInstanceOf(NoValidTokenException.class);

        DrivethruToken expiredRefresh = original.sessionCopy();
        expiredRefresh.setAccessToken("expired-session-jwt");
        expiredRefresh.setRefreshTokenTTL(1L);
        assertThatThrownBy(() -> service.streamOwnedProductBatches(expiredRefresh, batch -> { }))
                .isInstanceOf(NoValidTokenException.class);

        DrivethruToken rejectedRefresh = original.sessionCopy();
        rejectedRefresh.setAccessToken("expired-session-jwt");
        rejectedRefresh.setRefreshToken("rejected-refresh-token");
        assertThatThrownBy(() -> service.streamOwnedProductBatches(rejectedRefresh, batch -> { }))
                .isInstanceOf(NoValidTokenException.class);

        verify(1, postRequestedFor(urlPathEqualTo("/api/vBeta/auth_key")));
        verify(1, postRequestedFor(urlPathEqualTo("/api/vBeta/auth/refresh")));
    }

    private void assertMetrics(final String operation, final String outcome, final int count) {
        String[] tags = {"operation", operation, "outcome", outcome};
        assertThat(meterRegistry.get(DriveThruRPGClientConfig.REQUEST_COUNT).tags(tags).counter().count())
                .isEqualTo(count);
        var timer = meterRegistry.get(DriveThruRPGClientConfig.REQUEST_DURATION).tags(tags).timer();
        assertThat(timer.count()).isEqualTo(count);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isPositive();
    }

    private static String orderProductCollectionJson(final List<Integer> productIds, final String nextLink) {
        String items = productIds.stream()
                .map(id -> "{\"id\":\"/api/vBeta/order_products/" + id + "\","
                        + "\"type\":\"OrderProduct\","
                        + "\"attributes\":{\"orderId\":1,\"productId\":" + id + ","
                        + "\"royaltyPublisherId\":3444,\"name\":\"Product " + id + "\","
                        + "\"finalPrice\":0,\"quantity\":1,\"bundleId\":0,\"archived\":0,"
                        + "\"orderProductId\":" + id + ",\"customerId\":424242,\"files\":[]}}")
                .collect(Collectors.joining(","));
        String nextJson = nextLink == null ? "null" : "\"" + nextLink + "\"";
        return "{\"links\":{\"self\":\"/api/vBeta/order_products\",\"next\":" + nextJson + "},"
                + "\"meta\":{\"itemsPerPage\":" + PAGE_SIZE + ",\"currentPage\":1},"
                + "\"data\":[" + items + "]}";
    }
}
