package com.supportportal.repository;

import com.supportportal.entity.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, Long> {

    Optional<ComplaintCategory> findByNameIgnoreCase(String name);

    List<ComplaintCategory> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}
