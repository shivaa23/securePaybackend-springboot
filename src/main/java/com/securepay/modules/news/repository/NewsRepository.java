package com.securepay.modules.news.repository;

import com.securepay.modules.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link News}. */
public interface NewsRepository extends JpaRepository<News, Long> {
}
