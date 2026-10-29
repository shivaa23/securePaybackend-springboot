package com.securepay.modules.signup.service;

import com.securepay.modules.signup.entity.State;
import com.securepay.modules.signup.repository.StateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** CRUD service for {@link State}. */
@Service
public class StateService {

    private final StateRepository repository;

    public StateService(StateRepository repository) {
        this.repository = repository;
    }

    public List<State> findAll() {
        return repository.findAll();
    }

    public Optional<State> findById(Long id) {
        return repository.findById(id);
    }

    public State save(State e) {
        return repository.save(e);
    }

    public boolean deleteById(Long id) {
        if (repository.existsById(id)) { repository.deleteById(id); return true; }
        return false;
    }
}
