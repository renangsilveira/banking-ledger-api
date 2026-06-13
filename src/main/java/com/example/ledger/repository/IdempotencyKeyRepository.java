package com.example.ledger.repository;

import com.example.ledger.domain.idempotency.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByKey(String key);

    @Modifying
    @Query("DELETE FROM IdempotencyKey ik WHERE ik.expiresAt < :now")
    int deleteExpiredKeys(LocalDateTime now);
}
