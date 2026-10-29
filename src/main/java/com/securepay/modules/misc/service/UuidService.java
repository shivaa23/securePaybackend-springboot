package com.securepay.modules.misc.service;

import com.securepay.modules.misc.entity.Uuid;
import com.securepay.modules.misc.repository.UuidRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Port of {@code Uuid::check($uuid, $action, $request)}: a client_ref obtained from misc/getUuid
 * may be consumed exactly once. A missing or already-used uuid is rejected; on success the row is
 * marked used (status 0) and the request/action are recorded.
 */
@Service
public class UuidService {

    private final UuidRepository uuidRepository;

    public UuidService(UuidRepository uuidRepository) {
        this.uuidRepository = uuidRepository;
    }

    /** @return true when the uuid was valid and has now been consumed. */
    @Transactional
    public boolean consume(String uuid, String action, String requestJson) {
        if (uuid == null) {
            return false;
        }
        Optional<Uuid> row = uuidRepository.findFirstByUuid(uuid);
        if (row.isEmpty() || row.get().getStatus() == null || row.get().getStatus() != 1) {
            return false;
        }
        Uuid u = row.get();
        u.setStatus(0);
        u.setRequestData(requestJson);
        u.setAction(action);
        uuidRepository.save(u);
        return true;
    }
}
