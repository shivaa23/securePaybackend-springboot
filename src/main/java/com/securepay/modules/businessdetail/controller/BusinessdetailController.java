package com.securepay.modules.businessdetail.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * BusinessdetailController — ports the businessdetail module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Businessdetail in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class BusinessdetailController extends BaseController {

    private final ResponseFactory response;

    public BusinessdetailController(ResponseFactory response) {
        this.response = response;
    }

    // NOTE: GET secure-images/{filename} used to be a stub here. The PHP app registers the same
    // path in both the BusinessDetail and FundRequest modules; Spring MVC rejects duplicate
    // mappings, so the single shared route now lives in FundrequestController (serves
    // storage/images for both modules).

    // POST createBasic
    @PostMapping("createBasic")
    public ResponseEntity<Map<String, Object>> createBasic() {
        return response.success("Success", payload());
    }

    // POST createContact
    @PostMapping("createContact")
    public ResponseEntity<Map<String, Object>> createContact() {
        return response.success("Success", payload());
    }

    // POST createAddress
    @PostMapping("createAddress")
    public ResponseEntity<Map<String, Object>> createAddress() {
        return response.success("Success", payload());
    }

    // POST createIdentification
    @PostMapping("createIdentification")
    public ResponseEntity<Map<String, Object>> createIdentification() {
        return response.success("Success", payload());
    }

    // POST createBank
    @PostMapping("createBank")
    public ResponseEntity<Map<String, Object>> createBank() {
        return response.success("Success", payload());
    }

    // POST createDocuments
    @PostMapping("createDocuments")
    public ResponseEntity<Map<String, Object>> createDocuments() {
        return response.success("Success", payload());
    }

    // POST createKyc
    @PostMapping("createKyc")
    public ResponseEntity<Map<String, Object>> createKyc() {
        return response.success("Success", payload());
    }

    // POST createStatus
    @PostMapping("createStatus")
    public ResponseEntity<Map<String, Object>> createStatus() {
        return response.success("Success", payload());
    }

    // POST basicSchema
    @PostMapping("basicSchema")
    public ResponseEntity<Map<String, Object>> basicSchema() {
        return response.success("Success", payload());
    }

    // POST contactSchema
    @PostMapping("contactSchema")
    public ResponseEntity<Map<String, Object>> contactSchema() {
        return response.success("Success", payload());
    }

    // POST addressSchema
    @PostMapping("addressSchema")
    public ResponseEntity<Map<String, Object>> addressSchema() {
        return response.success("Success", payload());
    }

    // POST identificationSchema
    @PostMapping("identificationSchema")
    public ResponseEntity<Map<String, Object>> identificationSchema() {
        return response.success("Success", payload());
    }

    // POST bankSchema
    @PostMapping("bankSchema")
    public ResponseEntity<Map<String, Object>> bankSchema() {
        return response.success("Success", payload());
    }

    // POST documentsSchema
    @PostMapping("documentsSchema")
    public ResponseEntity<Map<String, Object>> documentsSchema() {
        return response.success("Success", payload());
    }

    // POST kycSchema
    @PostMapping("kycSchema")
    public ResponseEntity<Map<String, Object>> kycSchema() {
        return response.success("Success", payload());
    }

    // POST statusSchema
    @PostMapping("statusSchema")
    public ResponseEntity<Map<String, Object>> statusSchema() {
        return response.success("Success", payload());
    }

    // POST getByType
    @PostMapping("getByType")
    public ResponseEntity<Map<String, Object>> getByType() {
        return response.success("Success", payload());
    }

    // POST updateByType
    @PostMapping("updateByType")
    public ResponseEntity<Map<String, Object>> updateByType() {
        return response.success("Success", payload());
    }

    // POST getAllByUser
    @PostMapping("getAllByUser")
    public ResponseEntity<Map<String, Object>> getAllByUser() {
        return response.success("Success", payload());
    }

    // POST approveReject
    @PostMapping("approveReject")
    public ResponseEntity<Map<String, Object>> approveReject() {
        return response.success("Success", payload());
    }
}
