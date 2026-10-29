package com.securepay.modules.ipay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.ipay.entity.IpayDetail;
import com.securepay.modules.ipay.service.IpayDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/** IpayController — real CRUD, ported from app/Modules/Ipay (doc 05/08). */
@RestController
public class IpayController extends BaseController {

    private final ResponseFactory response;
    private final ObjectMapper objectMapper;
    private final IpayDetailService ipayDetailService;

    public IpayController(ResponseFactory response, ObjectMapper objectMapper, IpayDetailService ipayDetailService) {
        this.response = response;
        this.objectMapper = objectMapper;
        this.ipayDetailService = ipayDetailService;
    }

    // POST ipay/getIpay
    @PostMapping("ipay/getIpay")
    public ResponseEntity<Map<String, Object>> ipayGetIpay() {
        return response.success("IpayDetail fetched", ipayDetailService.findAll());
    }

    // POST ipay/updateIpay
    @PostMapping("ipay/updateIpay")
    public ResponseEntity<Map<String, Object>> ipayUpdateIpay() {
        Object idObj = payload().get("id");
        if (idObj == null) { return response.error("id is required", ""); }
        Optional<IpayDetail> ex = ipayDetailService.findById(Long.valueOf(idObj.toString()));
        if (ex.isEmpty()) { return response.error("IpayDetail not found", ""); }
        IpayDetail m = objectMapper.convertValue(payload(), IpayDetail.class);
        m.setId(ex.get().getId());
        return response.success("IpayDetail updated", ipayDetailService.save(m));
    }
}
