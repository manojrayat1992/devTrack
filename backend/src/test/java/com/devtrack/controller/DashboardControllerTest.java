package com.devtrack.controller;

import com.devtrack.dto.DashboardStatsResponse;
import com.devtrack.dto.SystemMetricsResponse;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import com.devtrack.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DashboardController Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/dashboard returns 200 with user task statistics")
    void getUserDashboardReturns200() throws Exception {
        DashboardStatsResponse stats = new DashboardStatsResponse(
            5,
            Map.of(TaskStatus.OPEN, 3L, TaskStatus.COMPLETED, 2L),
            Map.of(TaskPriority.HIGH, 2L, TaskPriority.LOW, 3L),
            1,
            0
        );

        when(dashboardService.getUserDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTasks").value(5))
            .andExpect(jsonPath("$.dueTodayCount").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/metrics returns 200 for user with ADMIN authority")
    void getAdminMetricsReturns200() throws Exception {
        SystemMetricsResponse metrics = new SystemMetricsResponse(
            50,
            12,
            Map.of(TaskStatus.COMPLETED, 25L),
            Map.of(TaskPriority.CRITICAL, 5L),
            25
        );

        when(dashboardService.getSystemMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/admin/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTasks").value(50))
            .andExpect(jsonPath("$.activeUsersCount").value(12));
    }
}
