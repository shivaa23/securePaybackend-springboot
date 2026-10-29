package com.securepay.modules.userdevice.service;

import com.securepay.modules.userdevice.entity.UserDevice;
import com.securepay.modules.userdevice.repository.UserDeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** CRUD service for {@link UserDevice}. */
@Service
public class UserDeviceService {

    private final UserDeviceRepository repository;

    public UserDeviceService(UserDeviceRepository repository) {
        this.repository = repository;
    }

    public List<UserDevice> findAll() {
        return repository.findAll();
    }

    public Optional<UserDevice> findById(Long id) {
        return repository.findById(id);
    }

    public UserDevice save(UserDevice e) {
        return repository.save(e);
    }

    public boolean deleteById(Long id) {
        if (repository.existsById(id)) { repository.deleteById(id); return true; }
        return false;
    }
}
