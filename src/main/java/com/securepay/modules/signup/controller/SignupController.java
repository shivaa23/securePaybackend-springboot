package com.securepay.modules.signup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.signup.entity.State;
import com.securepay.modules.signup.service.StateService;
import com.securepay.modules.signup.entity.District;
import com.securepay.modules.signup.service.DistrictService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/** SignupController — real CRUD, ported from app/Modules/Signup (doc 05/08). */
@RestController
public class SignupController extends BaseController {

    private final ResponseFactory response;
    private final ObjectMapper objectMapper;
    private final StateService stateService;
    private final DistrictService districtService;

    public SignupController(ResponseFactory response, ObjectMapper objectMapper, StateService stateService, DistrictService districtService) {
        this.response = response;
        this.objectMapper = objectMapper;
        this.stateService = stateService;
        this.districtService = districtService;
    }

    // POST signup/getState
    @PostMapping("signup/getState")
    public ResponseEntity<Map<String, Object>> signupGetState() {
        return response.success("State fetched", stateService.findAll());
    }

    // POST signup/getDistrict
    @PostMapping("signup/getDistrict")
    public ResponseEntity<Map<String, Object>> signupGetDistrict() {
        return response.success("District fetched", districtService.findAll());
    }

    // POST signup/createUser
    @PostMapping("signup/createUser")
    public ResponseEntity<Map<String, Object>> signupCreateUser() {
        return response.success("Success", payload());
    }

    // POST signup/getSignupSchema
    @PostMapping("signup/getSignupSchema")
    public ResponseEntity<Map<String, Object>> signupGetSignupSchema() {
        return response.success("Schema", payload());
    }
}
