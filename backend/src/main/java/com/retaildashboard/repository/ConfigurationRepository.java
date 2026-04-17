package com.retaildashboard.repository;

import com.retaildashboard.domain.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 설정 데이터 접근 Repository.
 *
 * Requirements: 17.1
 */
@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Integer> {

    Optional<Configuration> findByKey(String key);

    List<Configuration> findByIsRequiredTrue();

    boolean existsByKey(String key);
}
