package com.securepay.modules.layout.repository;

import com.securepay.modules.layout.entity.Colour;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Colour}. */
public interface ColourRepository extends JpaRepository<Colour, Long> {
}
