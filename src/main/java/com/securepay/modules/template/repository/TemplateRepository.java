package com.securepay.modules.template.repository;

import com.securepay.modules.template.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Template}. */
public interface TemplateRepository extends JpaRepository<Template, Long> {

    java.util.Optional<Template> findFirstByName(String name);
}
