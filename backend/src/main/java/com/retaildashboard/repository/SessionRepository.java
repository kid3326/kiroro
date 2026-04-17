package com.retaildashboard.repository;

import com.retaildashboard.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 세션 데이터 접근 Repository.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByToken(String token);

    List<Session> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
