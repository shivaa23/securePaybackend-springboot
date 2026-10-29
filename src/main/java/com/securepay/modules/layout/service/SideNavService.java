package com.securepay.modules.layout.service;

import com.securepay.modules.layout.entity.SideNav;
import com.securepay.modules.layout.repository.SideNavRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** CRUD service for {@link SideNav}. */
@Service
public class SideNavService {

    private final SideNavRepository repository;

    public SideNavService(SideNavRepository repository) {
        this.repository = repository;
    }

    public List<SideNav> findAll() {
        return repository.findAll();
    }

    public Optional<SideNav> findById(Long id) {
        return repository.findById(id);
    }

    public SideNav save(SideNav e) {
        return repository.save(e);
    }

    public boolean deleteById(Long id) {
        if (repository.existsById(id)) { repository.deleteById(id); return true; }
        return false;
    }
}
