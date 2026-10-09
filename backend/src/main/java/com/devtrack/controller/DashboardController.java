package com.devtrack.controller;

import com.devtrack.dto.DashboardStatsResponse;
import com.devtrack.dto.SystemMetricsResponse;
import com.devtrack.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Dashboard", description = "Endpoints for user statistics and administrative system metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get user dashboard statistics", description = "Returns task metrics for the authenticated user")
    public ResponseEntity<DashboardStatsResponse> getUserDashboard() {
        DashboardStatsResponse stats = dashboardService.getUserDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/admin/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get administrative system metrics", description = "Returns aggregated system-wide task statistics")
    public ResponseEntity<SystemMetricsResponse> getAdminMetrics() {
        SystemMetricsResponse metrics = dashboardService.getSystemMetrics();
        return ResponseEntity.ok(metrics);
    }
}
