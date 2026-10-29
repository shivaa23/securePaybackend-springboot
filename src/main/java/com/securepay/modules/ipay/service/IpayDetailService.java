package com.securepay.modules.ipay.service;

import com.securepay.modules.ipay.entity.IpayDetail;
import com.securepay.modules.ipay.repository.IpayDetailRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** CRUD service for {@link IpayDetail}. */
@Service
public class IpayDetailService {

    private final IpayDetailRepository repository;

    public IpayDetailService(IpayDetailRepository repository) {
        this.repository = repository;
    }

    public List<IpayDetail> findAll() {
        return repository.findAll();
    }

    public Optional<IpayDetail> findById(Long id) {
        return repository.findById(id);
    }

    public IpayDetail save(IpayDetail e) {
        return repository.save(e);
    }

    public boolean deleteById(Long id) {
        if (repository.existsById(id)) { repository.deleteById(id); return true; }
        return false;
    }
}
