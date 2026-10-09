package com.devtrack.config;

import com.devtrack.controller.DashboardController;
import com.devtrack.controller.TaskController;
import com.devtrack.service.DashboardService;
import com.devtrack.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TaskController.class, DashboardController.class})
@Import(SecurityConfig.class)
@DisplayName("Security & Role Access Control Integration Tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Authenticated user with USER role can access /api/dashboard")
    void authenticatedUserCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("User with USER role cannot access /api/admin/metrics (returns 403 Forbidden)")
    void userCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/metrics"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("User with ADMIN role can access /api/admin/metrics")
    void adminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/metrics"))
            .andExpect(status().isOk());
    }
}
