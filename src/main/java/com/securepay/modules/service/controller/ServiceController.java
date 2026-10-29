package com.securepay.modules.service.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Validation;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.service.repository.ServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Port of {@code Service/Controllers/ServiceController}: service catalogue CRUD. */
@RestController
public class ServiceController extends BaseController {

    private final ResponseFactory response;
    private final ServiceRepository repository;

    public ServiceController(ResponseFactory response, ServiceRepository repository) {
        this.response = response;
        this.repository = repository;
    }

    private static boolean isAdmin(CurrentUser cu) {
        return "sadm".equals(cu.user().getRole()) || "adm".equals(cu.user().getRole());
    }

    private static String str(Map<String, Object> p, String k) {
        return p.get(k) == null ? null : p.get(k).toString();
    }

    private static Integer num(Map<String, Object> p, String k) {
        String v = str(p, k);
        if (v == null || v.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(v)) {
            return 1;
        }
        if ("false".equalsIgnoreCase(v)) {
            return 0;
        }
        return Integer.valueOf(v.trim());
    }

    // POST service/getServices
    @PostMapping("service/getServices")
    public ResponseEntity<Map<String, Object>> getServices() {
        String subType = str(payload(), "sub_type");
        List<ServiceEntity> services = subType != null && !subType.isBlank()
                ? repository.findBySubType(subType) : repository.findAll();
        if (!services.isEmpty()) {
            return response.success("Services fetched", services);
        }
        return response.error("No Data Found", List.of(), HttpStatus.OK);
    }

    // POST service/createService
    @PostMapping("service/createService")
    public ResponseEntity<Map<String, Object>> createService(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("code").required().string().unique(repository::existsByCode);
        v.field("name").required().string();
        v.field("route").sometimes().string();
        v.field("category").sometimes().string();
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!isAdmin(cu)) {
            return response.error("Action not Authorised", "", HttpStatus.FORBIDDEN);
        }
        ServiceEntity s = new ServiceEntity();
        s.setName(str(p, "name"));
        s.setIsActiveApi(num(p, "is_active_api"));
        s.setIsActiveUsers(num(p, "is_active_users"));
        s.setCode(str(p, "code"));
        s.setIsActive(p.get("is_active") == null ? 1 : num(p, "is_active"));
        s.setRoute(str(p, "route") == null ? null : str(p, "route").toUpperCase());
        s.setCategory(str(p, "category"));
        s = repository.save(s);
        return response.success("Service Created Successfully", s);
    }

    // POST service/updateService
    @PostMapping("service/updateService")
    public ResponseEntity<Map<String, Object>> updateService(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("id").required().integer();
        v.field("code").sometimes().string();
        v.field("name").sometimes().string();
        v.field("is_active").sometimes().integer();
        v.field("route").sometimes().string();
        v.field("category").sometimes().string();
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!isAdmin(cu)) {
            return response.error("Action not Authorised", "", HttpStatus.FORBIDDEN);
        }
        ServiceEntity s = repository.findById(Long.valueOf(str(p, "id").trim())).orElse(null);
        if (s == null) {
            return response.error("Something Went Wrong", "", HttpStatus.FORBIDDEN);
        }
        if (p.get("name") != null) {
            s.setName(str(p, "name"));
        }
        if (p.get("code") != null) {
            s.setCode(str(p, "code"));
        }
        if (p.get("is_active_api") != null) {
            s.setIsActiveApi(num(p, "is_active_api"));
        }
        if (p.get("is_active_users") != null) {
            s.setIsActiveUsers(num(p, "is_active_users"));
        }
        if (str(p, "route") != null && !str(p, "route").isBlank()) {
            s.setRoute(str(p, "route").toUpperCase());
        }
        if (str(p, "category") != null && !str(p, "category").isBlank()) {
            s.setCategory(str(p, "category"));
        }
        if (p.get("is_active") != null) {
            s.setIsActive(num(p, "is_active"));
        }
        s = repository.save(s);
        return response.success("Service updated Successfully", s);
    }

    // POST service/blockUnblock
    @PostMapping("service/blockUnblock")
    public ResponseEntity<Map<String, Object>> blockUnblock(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("id").required().integer().exists(id -> repository.existsById(Long.valueOf(id.trim())));
        v.field("is_active").sometimes().integer().in("0", "1");
        v.field("is_active_api").sometimes().integer().in("0", "1");
        v.field("is_active_users").sometimes().integer().in("0", "1");
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!isAdmin(cu)) {
            return response.error("Action not Authorised", "", HttpStatus.FORBIDDEN);
        }
        ServiceEntity s = repository.findById(Long.valueOf(str(p, "id").trim())).orElse(null);
        if (s == null) {
            return response.error("Service not found", "", HttpStatus.NOT_FOUND);
        }
        if (p.get("is_active") != null) {
            s.setIsActive(num(p, "is_active"));
        }
        if (p.get("is_active_api") != null) {
            s.setIsActiveApi(num(p, "is_active_api"));
        }
        if (p.get("is_active_users") != null) {
            s.setIsActiveUsers(num(p, "is_active_users"));
        }
        s = repository.save(s);
        return response.success("Service Status Changed Successfully", s);
    }
}
