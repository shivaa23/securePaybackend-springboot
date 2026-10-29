package com.securepay.modules.misc.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.modules.misc.entity.Uuid;
import com.securepay.modules.misc.repository.UuidRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MiscController — ports the misc module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Misc in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class MiscController extends BaseController {

    private static final DateTimeFormatter UUID_TS = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final ResponseFactory response;
    private final UuidRepository uuidRepository;

    public MiscController(ResponseFactory response, UuidRepository uuidRepository) {
        this.response = response;
        this.uuidRepository = uuidRepository;
    }

    // POST misc/apiBalance
    @PostMapping("misc/apiBalance")
    public ResponseEntity<Map<String, Object>> miscApiBalance() {
        return response.success("Success", payload());
    }

    // POST misc/apiBalanceNew
    @PostMapping("misc/apiBalanceNew")
    public ResponseEntity<Map<String, Object>> miscApiBalanceNew() {
        return response.success("Success", payload());
    }

    // POST misc/chkUuid
    @PostMapping("misc/chkUuid")
    public ResponseEntity<Map<String, Object>> miscChkUuid() {
        return response.success("Success", payload());
    }

    // GET misc/logoutAll
    @GetMapping("misc/logoutAll")
    public ResponseEntity<Map<String, Object>> getMiscLogoutAll() {
        return response.success("Success", payload());
    }

    /**
     * GET misc/getUuid — issues a client reference id: {@code yyMMddHHmmss} (IST) + 5 random
     * digits, persisted in {@code uuids} for the caller. The frontend uses it as client_ref.
     */
    @GetMapping("misc/getUuid")
    public ResponseEntity<Map<String, Object>> getMiscGetUuid(
            @AuthenticationPrincipal CurrentUser currentUser) {
        String time = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(UUID_TS);
        String uuid = time + ThreadLocalRandom.current().nextInt(10000, 100000);

        Uuid rec = new Uuid();
        rec.setUuid(uuid);
        rec.setStatus(1);
        rec.setUserId(currentUser == null || currentUser.user() == null ? null : currentUser.user().getId());
        uuidRepository.save(rec);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uuid", rec.getUuid());
        data.put("status", rec.getStatus());
        data.put("user_id", rec.getUserId());
        data.put("id", rec.getId());
        return response.success("Success", data);
    }
}
