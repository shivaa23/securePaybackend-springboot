package com.securepay.modules.plan.service;

import com.securepay.modules.plan.entity.Plan;
import com.securepay.modules.plan.repository.PlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Business logic for the plan module (CRUD over {@link Plan}). */
@Service
public class PlanService {

    private final PlanRepository repository;

    public PlanService(PlanRepository repository) {
        this.repository = repository;
    }

    public List<Plan> findAll() {
        return repository.findAll();
    }

    public Optional<Plan> findById(Long id) {
        return repository.findById(id);
    }

    public Plan save(Plan entity) {
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
