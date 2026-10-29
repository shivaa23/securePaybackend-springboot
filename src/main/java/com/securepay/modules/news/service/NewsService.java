package com.securepay.modules.news.service;

import com.securepay.modules.news.entity.News;
import com.securepay.modules.news.repository.NewsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Business logic for the news module (CRUD over {@link News}). */
@Service
public class NewsService {

    private final NewsRepository repository;

    public NewsService(NewsRepository repository) {
        this.repository = repository;
    }

    public List<News> findAll() {
        return repository.findAll();
    }

    public Optional<News> findById(Long id) {
        return repository.findById(id);
    }

    public News save(News entity) {
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
