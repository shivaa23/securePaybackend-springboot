package com.securepay.modules.kyc.service;

import com.securepay.modules.kyc.entity.Kyc;
import com.securepay.modules.kyc.repository.KycRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Business logic for the kyc module (CRUD over {@link Kyc}). */
@Service
public class KycService {

    private final KycRepository repository;

    public KycService(KycRepository repository) {
        this.repository = repository;
    }

    public List<Kyc> findAll() {
        return repository.findAll();
    }

    public Optional<Kyc> findById(Long id) {
        return repository.findById(id);
    }

    public Kyc save(Kyc entity) {
        return repository.save(entity);
    }

    public boolean deleteById(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
