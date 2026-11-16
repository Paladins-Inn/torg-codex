package de.paladinsinn.security;

import brave.Tracing;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.paladinsinn.drivethru.ApiKeyFingerprint;
import de.paladinsinn.drivethru.DriveThruLibrarySnapshot;
import de.paladinsinn.drivethru.DriveThruRPGService;
import de.paladinsinn.drivethru.LibraryLoadCoordinator;
import de.paladinsinn.drivethru.token.DrivethruToken;
import de.paladinsinn.drivethru.token.NoValidTokenException;
import de.paladinsinn.torg.codex.application.port.in.CatalogPublicationReference;
import de.paladinsinn.torg.codex.application.port.in.CatalogReferenceQuery;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies bulk ownership resolution and real authentication spans without exporting caller data.
 */
class DriveThruAuthenticationProviderTest {

    private static final String KEY = "test-application-key";
    private final DriveThruRPGService service = mock(DriveThruRPGService.class);
    private final CatalogReferenceQuery references = mock(CatalogReferenceQuery.class);
    private final ObservationRegistry observations = ObservationRegistry.create();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final List<MutableSpan> spans = new ArrayList<>();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(DriveThruAuthenticationProvider.class);
    private Level originalLevel;
    private Tracing tracing;
    private DriveThruAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        tracing = Tracing.newBuilder().sampler(Sampler.ALWAYS_SAMPLE).addSpanHandler(new SpanHandler() {
            @Override
            public boolean end(final TraceContext context, final MutableSpan span, final Cause cause) {
                spans.add(span);
                return true;
            }
        }).build();
        observations.observationConfig().observationHandler(new DefaultMeterObservationHandler(meters))
                .observationHandler(new DefaultTracingObservationHandler(
                new BraveTracer(tracing.tracer(), new BraveCurrentTraceContext(tracing.currentTraceContext()))));
        provider = new DriveThruAuthenticationProvider(service, references);
        provider.setObservationRegistry(observations);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logger.setLevel(originalLevel);
        logs.stop();
        tracing.close();
        meters.close();
    }

    @Test
    void logsStartAndCreatesChildSpanCoveringTokenLibraryAndOneBulkLookup() throws Exception {
        DrivethruToken token = mock(DrivethruToken.class);
        when(service.getToken(KEY)).thenAnswer(invocation -> {
            assertThat(logs.list.getFirst().getFormattedMessage()).isEqualTo("DriveThruRPG authentication started.");
            assertThat(tracing.tracer().currentSpan()).isNotNull();
            return token;
        });
        when(service.getIdsOfOwnedProducts(token)).thenReturn(List.of(1, 2, 1));
        var book = new CatalogPublicationReference(UUID.randomUUID(), "Core Rules", "core-rulebook");
        when(references.findPublicationsByProductIds(Set.of(1, 2))).thenReturn(List.of(book, book));
        brave.Span parent = tracing.tracer().nextSpan().name("login-request").start();
        try (brave.Tracer.SpanInScope ignored = tracing.tracer().withSpanInScope(parent)) {
            var result = provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(KEY));
            assertThat(result.isAuthenticated()).isTrue();
            assertThat(((DriveThruUserDetails) result.getPrincipal()).ownedCodexIds())
                    .containsExactly("core-rulebook");
            assertThat(tracing.tracer().currentSpan().context()).isEqualTo(parent.context());
        } finally {
            parent.finish();
        }

        verify(references).findPublicationsByProductIds(Set.of(1, 2));
        verifyNoMoreInteractions(references);
        MutableSpan authentication = spans.stream().filter(span -> span.name().equals("drivethru authenticate"))
                .findFirst().orElseThrow();
        assertThat(authentication.parentId()).isEqualTo(parent.context().spanIdString());
        assertThat(authentication.traceId()).isEqualTo(parent.context().traceIdString());
        assertThat(authentication.tags()).containsEntry("outcome", "success");
        assertThat(meters.get("drivethru.authentication").tag("outcome", "success").timer().count()).isEqualTo(1);
        assertThat(authentication.finishTimestamp()).isGreaterThanOrEqualTo(authentication.startTimestamp());
        assertThat(observations.getCurrentObservation()).isNull();
        assertThat(tracing.tracer().currentSpan()).isNull();
        assertThat(logs.list.getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.list.getFirst().getKeyValuePairs()).anySatisfy(pair -> {
            assertThat(pair.key).isEqualTo("event.action");
            assertThat(pair.value).isEqualTo("drivethru-auth-started");
        });
    }

    @Test
    void rejectedAuthenticationFinishesSpanWithoutExposingExceptionSecrets() throws Exception {
        when(service.getToken(KEY)).thenThrow(new NoValidTokenException());

        assertThatThrownBy(() -> provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(KEY)))
                .isInstanceOf(BadCredentialsException.class);

        assertSafeFailureSpan("rejected");
        verifyNoInteractions(references);
    }

    @Test
    void upstreamFailureFinishesSpanAndRetainsNoPartialOwnershipOrCredentials() throws Exception {
        DrivethruToken token = mock(DrivethruToken.class);
        when(service.getToken(KEY)).thenReturn(token);
        when(service.getIdsOfOwnedProducts(token)).thenThrow(
                new AuthenticationServiceException("upstream-url?applicationKey=" + KEY));

        assertThatThrownBy(() -> provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(KEY)))
                .isInstanceOf(AuthenticationServiceException.class);

        assertSafeFailureSpan("error");
        verifyNoInteractions(references);
    }

    @Test
    void delegatesToLibraryLoadCoordinatorOnCacheHitWithoutCallingRawLibraryService() throws Exception {
        LibraryLoadCoordinator coordinator = mock(LibraryLoadCoordinator.class);
        provider.setLibraryLoadCoordinator(coordinator);

        DrivethruToken token = new DrivethruToken();
        token.setAccessToken("jwt-provider-cache");
        token.setApplicationKey(KEY);
        when(service.getToken(KEY)).thenReturn(token);
        when(coordinator.loadCoordinated(ApiKeyFingerprint.of(KEY), token)).thenReturn(
                DriveThruLibrarySnapshot.of(List.of(284210), "424242", java.time.Instant.now()));
        when(references.findPublicationsByProductIds(Set.of(284210))).thenReturn(List.of(
                new CatalogPublicationReference(UUID.randomUUID(), "Aysle", "sourcebook-aysle")));

        var auth = provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(KEY));
        DriveThruUserDetails details = (DriveThruUserDetails) auth.getPrincipal();

        assertThat(details.ownedCodexIds()).containsExactly("sourcebook-aysle");
        assertThat(details.getCustomerId()).isEqualTo("424242");
        assertThat(details.token().getApplicationKey()).isNull();
        verify(coordinator).loadCoordinated(ApiKeyFingerprint.of(KEY), token);
        verify(service).getToken(KEY);
        verifyNoMoreInteractions(service);
    }

    private void assertSafeFailureSpan(final String outcome) {
        assertThat(spans).singleElement().satisfies(span -> {
            assertThat(span.tags()).containsEntry("outcome", outcome);
            assertThat(span.error()).hasMessage("DriveThruRPG authentication " + outcome).hasNoCause();
            assertThat(span.toString()).doesNotContain(KEY, "applicationKey", "upstream-url");
        });
        assertThat(logs.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs()).doesNotContain(KEY, "applicationKey");
            assertThat(event.getThrowableProxy()).isNull();
        });
        assertThat(observations.getCurrentObservation()).isNull();
        assertThat(tracing.tracer().currentSpan()).isNull();
        assertThat(meters.get("drivethru.authentication").tag("outcome", outcome).timer().count()).isEqualTo(1);
    }
}
