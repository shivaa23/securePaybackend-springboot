package com.securepay.modules.signup.service;

import com.securepay.modules.signup.entity.District;
import com.securepay.modules.signup.repository.DistrictRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** CRUD service for {@link District}. */
@Service
public class DistrictService {

    private final DistrictRepository repository;

    public DistrictService(DistrictRepository repository) {
        this.repository = repository;
    }

    public List<District> findAll() {
        return repository.findAll();
    }

    public Optional<District> findById(Long id) {
        return repository.findById(id);
    }

    public District save(District e) {
        return repository.save(e);
    }

    public boolean deleteById(Long id) {
        if (repository.existsById(id)) { repository.deleteById(id); return true; }
        return false;
    }
}
