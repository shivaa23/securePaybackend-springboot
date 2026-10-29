package com.securepay.modules.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.notification.entity.Notification;
import com.securepay.modules.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * NotificationController — real CRUD over {@link Notification} (notifications), ported from
 * app/Modules/Notification (ResponseHelper success/error envelopes, doc 08).
 */
@RestController
public class NotificationController extends BaseController {

    private final ResponseFactory response;
    private final NotificationService service;
    private final ObjectMapper objectMapper;

    public NotificationController(ResponseFactory response, NotificationService service, ObjectMapper objectMapper) {
        this.response = response;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // POST notification/getNotification
    @PostMapping("notification/getNotification")
    public ResponseEntity<Map<String, Object>> getNotification() {
        return response.success("Notification fetched", service.findAll());
    }

    // POST notification/getNotificationSchema
    @PostMapping("notification/getNotificationSchema")
    public ResponseEntity<Map<String, Object>> getNotificationSchema() {
        return response.success("Schema", payload());
    }

    // POST notification/createNotification
    @PostMapping("notification/createNotification")
    public ResponseEntity<Map<String, Object>> createNotification() {
        Notification entity = objectMapper.convertValue(payload(), Notification.class);
        entity.setId(null);
        return response.success("Notification created", service.save(entity));
    }

    // POST notification/updateNotification
    @PostMapping("notification/updateNotification")
    public ResponseEntity<Map<String, Object>> updateNotification() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        Optional<Notification> existing = service.findById(Long.valueOf(idObj.toString()));
        if (existing.isEmpty()) {
            return response.error("Notification not found", "");
        }
        Notification merged = objectMapper.convertValue(payload(), Notification.class);
        merged.setId(existing.get().getId());
        return response.success("Notification updated", service.save(merged));
    }

    // POST notification/deleteNotification
    @PostMapping("notification/deleteNotification")
    public ResponseEntity<Map<String, Object>> deleteNotification() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        boolean ok = service.deleteById(Long.valueOf(idObj.toString()));
        return ok ? response.success("Notification deleted", "")
                : response.error("Notification not found", "");
    }
}
