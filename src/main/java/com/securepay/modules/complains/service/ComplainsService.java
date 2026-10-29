package com.securepay.modules.complains.service;

import com.securepay.modules.complains.entity.Complains;
import com.securepay.modules.complains.repository.ComplainsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Business logic for the complains module (CRUD over {@link Complains}). */
@Service
public class ComplainsService {

    private final ComplainsRepository repository;

    public ComplainsService(ComplainsRepository repository) {
        this.repository = repository;
    }

    public List<Complains> findAll() {
        return repository.findAll();
    }

    public Optional<Complains> findById(Long id) {
        return repository.findById(id);
    }

    public Complains save(Complains entity) {
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
