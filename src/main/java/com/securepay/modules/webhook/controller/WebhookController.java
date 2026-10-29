package com.securepay.modules.webhook.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WebhookController — ports the webhook module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Webhook in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class WebhookController extends BaseController {

    private final ResponseFactory response;

    public WebhookController(ResponseFactory response) {
        this.response = response;
    }

    // POST webhook/guruWebhook
    @PostMapping("webhook/guruWebhook")
    public ResponseEntity<Map<String, Object>> webhookGuruWebhook() {
        return response.success("Success", payload());
    }

    // POST webhook/solitekWebhook
    @PostMapping("webhook/solitekWebhook")
    public ResponseEntity<Map<String, Object>> webhookSolitekWebhook() {
        return response.success("Success", payload());
    }

    // POST webhook/indicoreWebhook
    @PostMapping("webhook/indicoreWebhook")
    public ResponseEntity<Map<String, Object>> webhookIndicoreWebhook() {
        return response.success("Success", payload());
    }

    // POST webhook/getWebhook
    @PostMapping("webhook/getWebhook")
    public ResponseEntity<Map<String, Object>> webhookGetWebhook() {
        return response.success("Success", payload());
    }

    // POST webhook/fingPayCmsApi
    @PostMapping("webhook/fingPayCmsApi")
    public ResponseEntity<Map<String, Object>> webhookFingPayCmsApi() {
        return response.success("Success", payload());
    }

    // POST webhook/digiKhataWebhook
    @PostMapping("webhook/digiKhataWebhook")
    public ResponseEntity<Map<String, Object>> webhookDigiKhataWebhook() {
        return response.success("Success", payload());
    }

    // POST webhook/abhyamWebhook
    @PostMapping("webhook/abhyamWebhook")
    public ResponseEntity<Map<String, Object>> webhookAbhyamWebhook() {
        return response.success("Success", payload());
    }

    // POST webhook/utkalWebhook
    @PostMapping("webhook/utkalWebhook")
    public ResponseEntity<Map<String, Object>> webhookUtkalWebhook() {
        return response.success("Success", payload());
    }

    // POST webhook/dynamicWebhook
    @PostMapping("webhook/dynamicWebhook")
    public ResponseEntity<Map<String, Object>> webhookDynamicWebhook() {
        return response.success("Success", payload());
    }
}
