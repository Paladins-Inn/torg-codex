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

import de.paladinsinn.security.ApiKeySessionAuthentication;
import de.paladinsinn.security.DriveThruLibrarySynchronizer;
import de.paladinsinn.torg.codex.web.codex.mapper.CodexViewMapper;
import de.paladinsinn.torg.codex.web.codex.model.AuthPageViewModel;
import de.paladinsinn.torg.codex.web.codex.model.NavContextViewModel;
import de.paladinsinn.torg.codex.web.notification.CsrfViewModel;
import de.paladinsinn.torg.codex.web.notification.ToastViewModel;
import de.paladinsinn.torg.codex.web.notification.WebNotificationSupport;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Inbound reader-UI authentication controller serving dedicated non-admin DriveThruRPG API-key
 * login and logout pages (FR-014, SC-006, ADR-022).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CodexAuthController {

    private static final String AUTH_FAILED_TITLE = "Authentication Failed";
    private static final String SERVICE_UNAVAILABLE_TITLE = "Service Unavailable";
    private static final String INVALID_KEY_MESSAGE =
            "Invalid DriveThruRPG API key. Please check your Application Key and try again.";
    private static final String UNAVAILABLE_MESSAGE =
            "DriveThruRPG is temporarily unavailable. Please try again later.";

    private final ApiKeySessionAuthentication sessionAuthentication;
    private final CodexViewMapper viewMapper;

    @Setter(onMethod_ = @Autowired(required = false))
    private WebNotificationSupport notificationSupport;

    @Setter(onMethod_ = @Autowired(required = false))
    private DriveThruLibrarySynchronizer synchronizer;

    @Timed(value = "torg.codex.web.page", extraTags = {"page", "login-view"})
    @GetMapping("/login")
    public String login(
            @RequestParam(name = "logout", required = false) final String logoutParam,
            final Authentication authentication,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Model model) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        model.addAttribute("model", buildAuthPageModel(authentication, request, null, logoutParam != null));
        return "codex/pages/login";
    }

    @Timed(value = "torg.codex.web.page", extraTags = {"page", "login-submit"})
    @PostMapping("/login")
    public String performLogin(
            @RequestParam(name = "apiKey", required = false) final String apiKey,
            final Authentication currentAuthentication,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Model model) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        if (apiKey == null || apiKey.isBlank()) {
            log.atInfo()
                    .addKeyValue("event.action", "user-login-rejected")
                    .addKeyValue("reason", "invalid-credentials")
                    .log("DriveThruRPG API-key login rejected");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            if (notificationSupport != null) {
                notificationSupport.addRequestErrorToast(request, AUTH_FAILED_TITLE, INVALID_KEY_MESSAGE);
            }
            model.addAttribute("model", buildAuthPageModel(currentAuthentication, request, INVALID_KEY_MESSAGE, false));
            return "codex/pages/login";
        }

        try {
            sessionAuthentication.authenticateInteractive(apiKey, null, request, response);
            if (notificationSupport != null) {
                notificationSupport.getOrCreateChannelId(request, true);
            }
            log.atInfo()
                    .addKeyValue("event.action", "user-login")
                    .log("DriveThruRPG session established");

            return "redirect:/";
        } catch (AuthenticationServiceException e) {
            log.atWarn()
                    .addKeyValue("event.action", "user-login-unavailable")
                    .log("DriveThruRPG login service unavailable");
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            if (notificationSupport != null) {
                notificationSupport.addRequestErrorToast(request, SERVICE_UNAVAILABLE_TITLE, UNAVAILABLE_MESSAGE);
            }
            model.addAttribute("model", buildAuthPageModel(
                    currentAuthentication,
                    request,
                    UNAVAILABLE_MESSAGE,
                    false));
            return "codex/pages/login";
        } catch (AuthenticationException e) {
            log.atInfo()
                    .addKeyValue("event.action", "user-login-rejected")
                    .addKeyValue("reason", "invalid-credentials")
                    .log("DriveThruRPG API-key login rejected");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            if (notificationSupport != null) {
                notificationSupport.addRequestErrorToast(request, AUTH_FAILED_TITLE, INVALID_KEY_MESSAGE);
            }
            model.addAttribute("model", buildAuthPageModel(currentAuthentication, request, INVALID_KEY_MESSAGE, false));
            return "codex/pages/login";
        }
    }

    @Timed(value = "torg.codex.web.page", extraTags = {"page", "logout-view"})
    @GetMapping("/logout")
    public String logoutPage(
            final Authentication authentication,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Model model) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        CsrfViewModel csrf = viewMapper.isSignedIn(authentication) ? CsrfViewModel.fromRequest(request) : null;
        NavContextViewModel baseNav = viewMapper.toNavContext(null, authentication);
        NavContextViewModel nav = baseNav != null && csrf != null ? baseNav.withCsrf(csrf) : baseNav;
        model.addAttribute("model", new AuthPageViewModel(nav, null, false, csrf));
        return "codex/pages/logout";
    }

    @Timed(value = "torg.codex.web.page", extraTags = {"page", "library-reload"})
    @PostMapping("/library/reload")
    public String reloadLibrary(
            final Authentication authentication,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Model model) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        boolean htmxRequest = "true".equalsIgnoreCase(request.getHeader("HX-Request"));
        var session = request.getSession(false);
        if (!viewMapper.isSignedIn(authentication) || session == null || synchronizer == null) {
            return redirectAnonymousReload(htmxRequest, response);
        }

        String channelId = notificationSupport != null
                ? notificationSupport.getOrCreateChannelId(request, true)
                : null;
        boolean started = synchronizer.startManualReload(
                session,
                channelId,
                sessionAuthentication.getContextRepository(),
                request,
                response);
        if (!started) {
            return redirectAnonymousReload(htmxRequest, response);
        }

        log.atInfo()
                .addKeyValue("event.action", "user-library-reload")
                .log("DriveThruRPG manual library reload started");

        if (htmxRequest) {
            List<ToastViewModel> toasts = notificationSupport != null
                    ? notificationSupport.buildReaderToastRegion(request, true).initialToasts()
                    : List.of();
            model.addAttribute("toasts", toasts);
            return "codex/fragments/toasts";
        }
        return "redirect:" + resolveSafeRefererPath(request);
    }

    private static String redirectAnonymousReload(
            final boolean htmxRequest,
            final HttpServletResponse response) {
        if (htmxRequest) {
            response.setStatus(HttpStatus.OK.value());
            response.setHeader("HX-Redirect", "/login");
            return null;
        }
        return "redirect:/login";
    }

    private static String resolveSafeRefererPath(final HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer.strip());
            if (uri.getScheme() != null && !Objects.equals(uri.getScheme(), request.getScheme())) {
                return "/";
            }
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return "/";
            }
            int uriPort = uri.getPort();
            if (uriPort != -1 && uriPort != request.getServerPort()) {
                return "/";
            }
            String rawPath = uri.getRawPath();
            if (rawPath == null || rawPath.isEmpty()
                    || !rawPath.startsWith("/")
                    || rawPath.startsWith("//")
                    || rawPath.contains("\\")
                    || "/login".equals(rawPath)
                    || "/logout".equals(rawPath)) {
                return "/";
            }
            String rawQuery = uri.getRawQuery();
            String candidate = rawQuery != null && !rawQuery.isEmpty() ? rawPath + "?" + rawQuery : rawPath;
            if (candidate.chars().anyMatch(Character::isISOControl)) {
                return "/";
            }
            return candidate;
        } catch (IllegalArgumentException malformed) {
            return "/";
        }
    }

    private AuthPageViewModel buildAuthPageModel(
            final Authentication authentication,
            final HttpServletRequest request,
            final String errorMessage,
            final boolean signedOutBanner) {
        CsrfViewModel csrf = CsrfViewModel.fromRequest(request);
        NavContextViewModel baseNav = viewMapper.toNavContext(null, authentication);
        if (baseNav != null) {
            baseNav = baseNav.withCsrf(csrf);
            if (notificationSupport != null) {
                baseNav = baseNav.withToastRegion(
                        notificationSupport.buildReaderToastRegion(request, viewMapper.isSignedIn(authentication)));
            }
        }
        return new AuthPageViewModel(baseNav, errorMessage, signedOutBanner, csrf);
    }
}
