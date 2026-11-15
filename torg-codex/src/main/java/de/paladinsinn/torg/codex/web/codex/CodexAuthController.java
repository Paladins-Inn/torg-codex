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
import de.paladinsinn.torg.codex.web.codex.mapper.CodexViewMapper;
import de.paladinsinn.torg.codex.web.codex.model.AuthPageViewModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String INVALID_KEY_MESSAGE =
            "Invalid DriveThruRPG API key. Please check your Application Key and try again.";

    private final ApiKeySessionAuthentication sessionAuthentication;
    private final CodexViewMapper viewMapper;

    @GetMapping("/login")
    public String login(
            @RequestParam(name = "logout", required = false) final String logoutParam,
            final Authentication authentication,
            final HttpServletResponse response,
            final Model model) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        model.addAttribute("model", new AuthPageViewModel(
                viewMapper.toNavContext(null, authentication),
                null,
                logoutParam != null));
        return "codex/pages/login";
    }

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
            model.addAttribute("model", new AuthPageViewModel(
                    viewMapper.toNavContext(null, currentAuthentication),
                    INVALID_KEY_MESSAGE,
                    false));
            return "codex/pages/login";
        }

        try {
            sessionAuthentication.authenticate(apiKey, request, response);
            log.atInfo()
                    .addKeyValue("event.action", "user-login")
                    .log("DriveThruRPG session established");

            return "redirect:/";
        } catch (AuthenticationServiceException e) {
            log.atWarn()
                    .addKeyValue("event.action", "user-login-unavailable")
                    .log("DriveThruRPG login service unavailable");
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            model.addAttribute("model", new AuthPageViewModel(
                    viewMapper.toNavContext(null, currentAuthentication),
                    "DriveThruRPG is temporarily unavailable. Please try again later.",
                    false));
            return "codex/pages/login";
        } catch (AuthenticationException e) {
            log.atInfo()
                    .addKeyValue("event.action", "user-login-rejected")
                    .addKeyValue("reason", "invalid-credentials")
                    .log("DriveThruRPG API-key login rejected");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            model.addAttribute("model", new AuthPageViewModel(
                    viewMapper.toNavContext(null, currentAuthentication),
                    INVALID_KEY_MESSAGE,
                    false));
            return "codex/pages/login";
        }
    }

    @GetMapping("/logout")
    public String logoutPage(
            final Authentication authentication,
            final HttpServletResponse response,
            final Model model) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        model.addAttribute("model", new AuthPageViewModel(
                viewMapper.toNavContext(null, authentication),
                null,
                false));
        return "codex/pages/logout";
    }
}
