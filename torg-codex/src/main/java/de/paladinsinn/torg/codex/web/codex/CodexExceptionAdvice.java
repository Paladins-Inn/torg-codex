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

import de.paladinsinn.torg.codex.web.codex.mapper.CodexViewMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Reader-UI-scoped exception handler rendering safe HTML error screens and logging unexpected
 * technical failures at {@code ERROR} per ADR-022 without affecting {@code /api/*} REST controllers.
 */
@ControllerAdvice(basePackages = "de.paladinsinn.torg.codex.web.codex")
@RequiredArgsConstructor
@Slf4j
public class CodexExceptionAdvice {

    private final CodexViewMapper viewMapper;

    @ExceptionHandler(Exception.class)
    public String handleUnexpectedException(
            final Exception exception,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication,
            final Model model) {
        log.atError()
                .addKeyValue("requestPath", request.getRequestURI())
                .addKeyValue("exceptionClass", exception.getClass().getName())
                .setCause(exception)
                .log("Unhandled technical failure in reader UI controller");
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        model.addAttribute("model", viewMapper.toNotFoundViewModel(
                null,
                "Unexpected Error",
                "An unexpected error occurred while rendering this page.",
                authentication));
        return "codex/pages/notFound";
    }
}
