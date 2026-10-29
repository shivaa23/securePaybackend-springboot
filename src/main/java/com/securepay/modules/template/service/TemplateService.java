package com.securepay.modules.template.service;

import com.securepay.modules.template.entity.Template;
import com.securepay.modules.template.repository.TemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Business logic for the template module (CRUD over {@link Template}). */
@Service
public class TemplateService {

    private final TemplateRepository repository;

    public TemplateService(TemplateRepository repository) {
        this.repository = repository;
    }

    public List<Template> findAll() {
        return repository.findAll();
    }

    public Optional<Template> findById(Long id) {
        return repository.findById(id);
    }

    public Template save(Template entity) {
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
