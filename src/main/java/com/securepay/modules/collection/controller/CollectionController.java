package com.securepay.modules.collection.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CollectionController — ports the collection module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Collection in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class CollectionController extends BaseController {

    private final ResponseFactory response;

    public CollectionController(ResponseFactory response) {
        this.response = response;
    }

    // POST collections/getCollections
    @PostMapping("collections/getCollections")
    public ResponseEntity<Map<String, Object>> collectionsGetCollections() {
        return response.success("Success", payload());
    }

    // POST collections/claim
    @PostMapping("collections/claim")
    public ResponseEntity<Map<String, Object>> collectionsClaim() {
        return response.success("Success", payload());
    }

    // POST collections/getCollections2
    @PostMapping("collections/getCollections2")
    public ResponseEntity<Map<String, Object>> collectionsGetCollections2() {
        return response.success("Success", payload());
    }

    // POST collections/claim2
    @PostMapping("collections/claim2")
    public ResponseEntity<Map<String, Object>> collectionsClaim2() {
        return response.success("Success", payload());
    }

    // POST collections/markUnclaimed2
    @PostMapping("collections/markUnclaimed2")
    public ResponseEntity<Map<String, Object>> collectionsMarkUnclaimed2() {
        return response.success("Success", payload());
    }

    // POST collections/claim3
    @PostMapping("collections/claim3")
    public ResponseEntity<Map<String, Object>> collectionsClaim3() {
        return response.success("Success", payload());
    }

    // POST collections/getCollections3
    @PostMapping("collections/getCollections3")
    public ResponseEntity<Map<String, Object>> collectionsGetCollections3() {
        return response.success("Success", payload());
    }

    // POST collections/checkTxnStatus
    @PostMapping("collections/checkTxnStatus")
    public ResponseEntity<Map<String, Object>> collectionsCheckTxnStatus() {
        return response.success("Success", payload());
    }

    // POST qr/initiateDynamicQrCollection
    @PostMapping("qr/initiateDynamicQrCollection")
    public ResponseEntity<Map<String, Object>> qrInitiateDynamicQrCollection() {
        return response.success("Success", payload());
    }

    // GET collections/expirePending
    @GetMapping("collections/expirePending")
    public ResponseEntity<Map<String, Object>> getCollectionsExpirePending() {
        return response.success("Success", payload());
    }
}
