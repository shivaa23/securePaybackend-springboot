package com.securepay.modules.dashboard.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * DashboardController — ports the dashboard module endpoints at their exact paths (doc 05).
 *
 * <p>These endpoints are not yet backed by real aggregation logic. The frontend dashboard
 * widgets iterate over {@code data} with {@code .map(...)}, so each endpoint returns an empty
 * JSON array ({@code []}) instead of the request echo — this keeps the UI from crashing with
 * "x.map is not a function" and simply shows "No data available" until the real queries land.</p>
 */
@RestController
public class DashboardController extends BaseController {

    private final ResponseFactory response;

    public DashboardController(ResponseFactory response) {
        this.response = response;
    }

    // POST dashboard/getRoleWiseCounts
    @PostMapping("dashboard/getRoleWiseCounts")
    public ResponseEntity<Map<String, Object>> dashboardGetRoleWiseCounts() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getServiceWiseSummary
    @PostMapping("dashboard/getServiceWiseSummary")
    public ResponseEntity<Map<String, Object>> dashboardGetServiceWiseSummary() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getRetTxnSummary
    @PostMapping("dashboard/getRetTxnSummary")
    public ResponseEntity<Map<String, Object>> dashboardGetRetTxnSummary() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getServiceWiseProfitSummaryForAdmin
    @PostMapping("dashboard/getServiceWiseProfitSummaryForAdmin")
    public ResponseEntity<Map<String, Object>> dashboardGetServiceWiseProfitSummaryForAdmin() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getServiceWiseProfitSummary
    @PostMapping("dashboard/getServiceWiseProfitSummary")
    public ResponseEntity<Map<String, Object>> dashboardGetServiceWiseProfitSummary() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getServiceWiseProfitSummaryNew
    @PostMapping("dashboard/getServiceWiseProfitSummaryNew")
    public ResponseEntity<Map<String, Object>> dashboardGetServiceWiseProfitSummaryNew() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getServiceWiseProfitSummaryForDi
    @PostMapping("dashboard/getServiceWiseProfitSummaryForDi")
    public ResponseEntity<Map<String, Object>> dashboardGetServiceWiseProfitSummaryForDi() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getServiceWiseProfitSummaryForMd
    @PostMapping("dashboard/getServiceWiseProfitSummaryForMd")
    public ResponseEntity<Map<String, Object>> dashboardGetServiceWiseProfitSummaryForMd() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getUserServiceWiseTxnReport
    @PostMapping("dashboard/getUserServiceWiseTxnReport")
    public ResponseEntity<Map<String, Object>> dashboardGetUserServiceWiseTxnReport() {
        return response.success("Success", List.of());
    }

    // POST dashboard/getTxnSummaryServiceWise
    @PostMapping("dashboard/getTxnSummaryServiceWise")
    public ResponseEntity<Map<String, Object>> dashboardGetTxnSummaryServiceWise() {
        return response.success("Success", List.of());
    }
}
