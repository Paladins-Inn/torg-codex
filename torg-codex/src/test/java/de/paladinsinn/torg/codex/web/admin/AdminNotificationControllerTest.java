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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

package de.paladinsinn.torg.codex.web.admin;

import de.paladinsinn.torg.codex.application.port.in.NotificationAction;
import de.paladinsinn.torg.codex.application.port.in.NotificationSeverity;
import de.paladinsinn.torg.codex.application.port.in.UserNotification;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationCommand;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationQuery;
import de.paladinsinn.torg.codex.application.port.in.UserNotificationRequested;
import de.paladinsinn.torg.codex.web.notification.CsrfViewModel;
import de.paladinsinn.torg.codex.web.notification.ToastRegionViewModel;
import de.paladinsinn.torg.codex.web.notification.WebNotificationSupport;
import gg.jte.Content;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@link AdminNotificationController} and isolated dark-theme toast rendering
 * on the {@code /admin} layout surface (US2, FR-005, FR-019, ADR-024).
 */
@Import(de.paladinsinn.torg.codex.TestcontainersConfiguration.class)
@SpringBootTest
class AdminNotificationControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserNotificationCommand notificationCommand;

    @Autowired
    private UserNotificationQuery notificationQuery;

    @Autowired
    private WebNotificationSupport notificationSupport;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TemplateEngine templateEngine;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminStreamAndAckEnforceIdenticalSecurityContractsAndIsolatedDarkThemeRendering() throws Exception {
        MvcResult anonGet = mockMvc.perform(get("/admin/notifications/stream"))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(anonGet.getRequest().getSession(false)).isNull();

        mockMvc.perform(post("/admin/notifications/ack").param("id", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());

        MockHttpSession adminSession = new MockHttpSession();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setSession(adminSession);
        String channelId = notificationSupport.getOrCreateChannelId(req, true);

        UserNotification info = notificationCommand.notifyInfo(
                channelId, "Index Rebuild", "Rebuilding <img src=x onerror=alert(1)>");
        UserNotification success = notificationCommand.notifySuccess(
                channelId, "Index Ready", "Completed", NotificationAction.reloadCurrentPage("Reload view"));
        UserNotification warning = notificationCommand.notifyWarning(
                channelId, "Quota Warning", "80% cache capacity");
        UserNotification error = notificationCommand.notifyError(
                channelId, "Import Error", "CSV line 12 invalid",
                NotificationAction.navigateUrl("Open status", "/admin"));

        // Verify isolated admin layout template rendering
        ToastRegionViewModel adminRegion = notificationSupport.buildAdminToastRegion(req);
        StringOutput output = new StringOutput();
        Content body = out -> out.writeContent("<p>Admin shell body</p>");
        templateEngine.render("admin/layout/page.jte", Map.of(
                "title", "Admin Console",
                "csrf", new CsrfViewModel("_csrf", "X-CSRF-TOKEN", "masked-admin-token"),
                "toastRegion", adminRegion,
                "content", body), output);
        String renderedAdminHtml = output.toString();

        assertThat(renderedAdminHtml)
                .contains("codex-admin-theme",
                        "data-bs-theme=\"dark\"",
                        "id=\"admin-toast-container\"",
                        "data-stream-url=\"/admin/notifications/stream\"",
                        "data-ack-url=\"/admin/notifications/ack\"",
                        "&lt;img src=x onerror=alert(1)&gt;",
                        "href=\"/admin\"",
                        "data-toast-action=\"reload\"")
                .doesNotContain("codex-reader-theme", "<img src=x onerror=alert(1)>");

        // Verify SSE endpoint flushes pending items and pushes live Spring Events in admin theme
        MvcResult streamResult = mockMvc.perform(get("/admin/notifications/stream")
                        .session(adminSession)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andReturn();

        eventPublisher.publishEvent(UserNotificationRequested.broadcast(
                NotificationSeverity.INFO, "System Notice", "Broadcast delivered to admin channel", null));

        String sseData = streamResult.getResponse().getContentAsString();
        assertThat(sseData)
                .contains("event:toast",
                        info.id().toString(),
                        success.id().toString(),
                        warning.id().toString(),
                        error.id().toString(),
                        "Broadcast delivered to admin channel");

        // Forbidden Origin & malformed UUID rejected properly
        mockMvc.perform(post("/admin/notifications/ack")
                        .session(adminSession)
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .param("id", info.id().toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/notifications/ack")
                        .session(adminSession)
                        .param("id", "bad-uuid")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/admin/notifications/ack")
                        .session(adminSession)
                        .param("id", info.id().toString(), success.id().toString(),
                                warning.id().toString(), error.id().toString())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(notificationQuery.pending(channelId))
                .singleElement()
                .extracting(UserNotification::title)
                .isEqualTo("System Notice");
    }
}
