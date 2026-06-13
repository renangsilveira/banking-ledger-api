package com.example.ledger.repository;

import com.example.ledger.domain.transaction.TransactionEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, UUID> {

    @Query("""
        SELECT e FROM TransactionEntry e
        JOIN FETCH e.transaction t
        WHERE e.account.id = :accountId
        AND (:from IS NULL OR e.createdAt >= :from)
        AND (:to IS NULL OR e.createdAt <= :to)
        ORDER BY e.createdAt DESC
        """)
    Page<TransactionEntry> findByAccountId(
        @Param("accountId") UUID accountId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
}
