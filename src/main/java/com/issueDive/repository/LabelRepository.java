package com.issueDive.repository;

import com.issueDive.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label,Long> {
    boolean existsByNameIgnoreCase(String name);
    Label findByIdIgnoreCase(Long id);
}
