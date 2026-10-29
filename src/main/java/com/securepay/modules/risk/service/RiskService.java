package com.securepay.modules.risk.service;

import com.securepay.modules.risk.entity.BlackListedAcc;
import com.securepay.modules.risk.entity.WhiteListedAcc;
import com.securepay.modules.risk.repository.BlackListedAccRepository;
import com.securepay.modules.risk.repository.WhiteListedAccRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Whitelist/blacklist account management — fraud control (doc 02 module 32).
 * Also exposes screening used by the account blacklist/whitelist interceptors on DMT.
 */
@Service
public class RiskService {

    private final WhiteListedAccRepository whiteRepo;
    private final BlackListedAccRepository blackRepo;

    public RiskService(WhiteListedAccRepository whiteRepo, BlackListedAccRepository blackRepo) {
        this.whiteRepo = whiteRepo;
        this.blackRepo = blackRepo;
    }

    // ----- Whitelist -----
    public List<WhiteListedAcc> allWhite() {
        return whiteRepo.findAll();
    }

    public WhiteListedAcc saveWhite(WhiteListedAcc acc) {
        return whiteRepo.save(acc);
    }

    public Optional<WhiteListedAcc> findWhite(Long id) {
        return whiteRepo.findById(id);
    }

    public boolean deleteWhite(Long id) {
        if (whiteRepo.existsById(id)) {
            whiteRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean isWhitelisted(String accNo) {
        return whiteRepo.findByAccNo(accNo).isPresent();
    }

    // ----- Blacklist -----
    public List<BlackListedAcc> allBlack() {
        return blackRepo.findAll();
    }

    public BlackListedAcc saveBlack(BlackListedAcc acc) {
        return blackRepo.save(acc);
    }

    public Optional<BlackListedAcc> findBlack(Long id) {
        return blackRepo.findById(id);
    }

    public boolean deleteBlack(Long id) {
        if (blackRepo.existsById(id)) {
            blackRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean isBlacklisted(String accNo) {
        return blackRepo.findByAccNo(accNo).isPresent();
    }
}
