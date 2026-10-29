package com.securepay.modules.userdevice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.userdevice.entity.UserDevice;
import com.securepay.modules.userdevice.service.UserDeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/** UserDeviceController — real CRUD, ported from app/Modules/Userdevice (doc 05/08). */
@RestController
public class UserDeviceController extends BaseController {

    private final ResponseFactory response;
    private final ObjectMapper objectMapper;
    private final UserDeviceService userDeviceService;

    public UserDeviceController(ResponseFactory response, ObjectMapper objectMapper, UserDeviceService userDeviceService) {
        this.response = response;
        this.objectMapper = objectMapper;
        this.userDeviceService = userDeviceService;
    }

    // POST userDevice/getUserDevice
    @PostMapping("userDevice/getUserDevice")
    public ResponseEntity<Map<String, Object>> userDeviceGetUserDevice() {
        return response.success("UserDevice fetched", userDeviceService.findAll());
    }

    // POST userDevice/createUserDevice
    @PostMapping("userDevice/createUserDevice")
    public ResponseEntity<Map<String, Object>> userDeviceCreateUserDevice() {
        UserDevice e = objectMapper.convertValue(payload(), UserDevice.class);
        e.setId(null);
        return response.success("UserDevice created", userDeviceService.save(e));
    }

    // POST userDevice/updateBank
    @PostMapping("userDevice/updateBank")
    public ResponseEntity<Map<String, Object>> userDeviceUpdateBank() {
        Object idObj = payload().get("id");
        if (idObj == null) { return response.error("id is required", ""); }
        Optional<UserDevice> ex = userDeviceService.findById(Long.valueOf(idObj.toString()));
        if (ex.isEmpty()) { return response.error("UserDevice not found", ""); }
        UserDevice m = objectMapper.convertValue(payload(), UserDevice.class);
        m.setId(ex.get().getId());
        return response.success("UserDevice updated", userDeviceService.save(m));
    }

    // POST userDevice/deleteBank
    @PostMapping("userDevice/deleteBank")
    public ResponseEntity<Map<String, Object>> userDeviceDeleteBank() {
        Object idObj = payload().get("id");
        if (idObj == null) { return response.error("id is required", ""); }
        boolean ok = userDeviceService.deleteById(Long.valueOf(idObj.toString()));
        return ok ? response.success("UserDevice deleted", "") : response.error("UserDevice not found", "");
    }
}
