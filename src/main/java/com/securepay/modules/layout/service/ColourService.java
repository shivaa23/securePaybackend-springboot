package com.securepay.modules.layout.service;

import com.securepay.modules.layout.entity.Colour;
import com.securepay.modules.layout.repository.ColourRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** CRUD service for {@link Colour}. */
@Service
public class ColourService {

    private final ColourRepository repository;

    public ColourService(ColourRepository repository) {
        this.repository = repository;
    }

    public List<Colour> findAll() {
        return repository.findAll();
    }

    public Optional<Colour> findById(Long id) {
        return repository.findById(id);
    }

    public Colour save(Colour e) {
        return repository.save(e);
    }

    public boolean deleteById(Long id) {
        if (repository.existsById(id)) { repository.deleteById(id); return true; }
        return false;
    }
}
