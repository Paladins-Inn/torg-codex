# Contract: Application Ports, Library Caching, Session Secrets and ArchUnit Boundaries

**Feature**: `007-async-library-login` | **Date**: 2026-11-16 (revised)

---

## 1. Framework-Free Notification Ports (`torg-codex-application`)

Types in `de.paladinsinn.torg.codex.application..` stay free of `org.springframework..`, `jakarta..`, `io.micrometer..`, `de.paladinsinn.drivethru..` and `de.paladinsinn.security..`.

```java
package de.paladinsinn.torg.codex.application.port.in;

public interface UserNotificationCommand {
    UserNotification notifySession(String channelId, NotificationSeverity severity,
                                   String title, String message, NotificationAction action);
    List<UserNotification> notifyAllAuthenticated(NotificationSeverity severity,
                                                  String title, String message, NotificationAction action);
    List<UserNotification> dispatch(NotificationTarget target, NotificationSeverity severity,
                                    String title, String message, NotificationAction action);
    // default helpers: notifyInfo / notifySuccess / notifyWarning / notifyError
}

public interface UserNotificationQuery {
    List<UserNotification> pending(String channelId);              // <= 20, non-destructive
    void acknowledge(String channelId, Set<UUID> notificationIds); // idempotent, channel-scoped
}
```

```java
package de.paladinsinn.torg.codex.application.port.out;

public interface UserNotificationStorePort {
    void enqueue(String channelId, UserNotification notification);
    List<UserNotification> pending(String channelId, int limit);
    void acknowledge(String channelId, Set<UUID> notificationIds);
    void registerAuthenticatedChannel(String channelId);
    Set<String> authenticatedChannels();
    void clear(String channelId);
}
```

Background producers publish `UserNotificationRequested` (framework-free record) as a Spring application event; a listener adapter in `torg-codex` forwards it to `UserNotificationCommand`, and storing a notification emits `UserNotificationPublished` so `SseNotificationBroadcaster` pushes rendered toast fragments immediately over `SseEmitter` without browser polling.

---

## 2. Annotation-Based Library Cache (`de.paladinsinn.drivethru`, `ADR-015`)

### 2.1 Cache configuration

- `DriveThruLibraryCacheConfig.LIBRARY_CACHE = "drivethru.library"`, registered via `CacheManagerCustomizer<CaffeineCacheManager>#registerCustomCache` with `expireAfterWrite(ttl)` and `maximumSize(maxSize)`. `"drivethru.product"` and `"drivethru.publisher"` keep their current semantics.
- `libraryCacheKeyGenerator` (`KeyGenerator`): returns `ApiKeyFingerprint.value()` of the first argument; rejects other argument types. Annotations use `keyGenerator` only, never `key`.

### 2.2 `DriveThruLibraryCacheService`

```java
@Cacheable(cacheNames = LIBRARY_CACHE, keyGenerator = "libraryCacheKeyGenerator")
public DriveThruLibrarySnapshot loadLibrary(ApiKeyFingerprint fingerprint, DrivethruToken token)
        throws NoValidTokenException;

@CachePut(cacheNames = LIBRARY_CACHE, keyGenerator = "libraryCacheKeyGenerator")
public DriveThruLibrarySnapshot reloadLibraryStreaming(ApiKeyFingerprint fingerprint, DrivethruToken token,
                                                       LibraryBatchListener listener)
        throws NoValidTokenException;

public Optional<DriveThruLibrarySnapshot> findCachedLibrary(ApiKeyFingerprint fingerprint);
```

`LibraryBatchListener#onBatch(DriveThruLibraryBatch)` may throw `SupersededSyncException` to abort a stale run before any cache write.

**Guarantees**
- Header mode (`DriveThruAuthenticationProvider`) uses `loadLibrary`; repeated calls within the TTL issue zero `/order_products` requests (`FR-013`).
- Login cache-miss and manual reload use `reloadLibraryStreaming` on `drivethruSyncExecutor`.
- `LibraryLoadCoordinator` serializes fills and commits per fingerprint across the proxy return; `loadLibrary` rechecks the cache after acquiring coordination.
- Any exception (upstream failure, superseded, aborted, not renewable) → no cache write, previous entry intact (`FR-012`).
- Logout performs no eviction (`FR-011`).
- Tests use real Spring cache proxies, not only direct instances.

### 2.3 Session secret handling (`FR-010`, `FR-018`)

- `ApiKeyFingerprint.of(apiKey)` = lowercase hex SHA-256 of the stripped key; stored in the session and used as cache key.
- `DrivethruToken#sessionCopy()` clears `applicationKey`; only session copies enter `DriveThruUserDetails` / `SecurityContext` (login and header mode).
- `DriveThruRPGService.renewToken` re-exchanges the application key only if present; otherwise refresh-only, failing with `NoValidTokenException`.

### 2.4 Configuration properties

```yaml
drivethru:
  library:
    cache:
      ttl: ${DRIVETHRU_LIBRARY_CACHE_TTL:12h}
      max-size: ${DRIVETHRU_LIBRARY_CACHE_MAX_SIZE:1000}
    sync:
      executor:
        core-size: ${DRIVETHRU_LIBRARY_SYNC_THREADS:4}
        queue-capacity: ${DRIVETHRU_LIBRARY_SYNC_QUEUE:100}
torg:
  codex:
    notifications:
      sse-timeout: ${TORG_CODEX_NOTIFICATION_SSE_TIMEOUT:30m}
    web:
      cors:
        allowed-origins: ${TORG_CODEX_WEB_CORS_ALLOWED_ORIGINS:}
management:
  observations:
    annotations:
      enabled: true
```

---

## 3. Architecture Invariants

1. Domain and application purity (`ApplicationPurityArchitectureTest`, `ModuleBoundaryArchitectureTest`).
2. Controllers only in `web.codex` / `web.admin`; shared `ToastViewModel`, `ToastRegionViewModel`, `CsrfViewModel`, mapper and session helpers in `web.notification`; no `web.codex` ↔ `web.admin` imports (`AdapterConventionArchitectureTest`, `WebUiSurfaceSeparationArchitectureTest`).
3. Single ownership mechanism: batches write `ROLE_<codexId>` via `DriveThruUserDetails`; `web..` never uses `GrantedAuthority`/`ProductOwnershipResolver` (`CensoringSingleMechanismArchitectureTest`).
4. DRM reads remain via `RestrictedCatalogQuery` (`CatalogDrmArchitectureTest`); publication resolution uses the existing `CatalogReferenceQuery`.
5. New ArchUnit rule: no class outside `de.paladinsinn.drivethru..` / `de.paladinsinn.security..` calls `DrivethruToken#getApplicationKey`.
