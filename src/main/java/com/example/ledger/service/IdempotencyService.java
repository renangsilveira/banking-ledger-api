package com.example.ledger.service;

import com.example.ledger.domain.idempotency.IdempotencyKey;
import com.example.ledger.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository,
                              ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    public <T> T executeIdempotent(
            String key,
            String entityType,
            Class<T> responseType,
            Supplier<IdempotencyResult<T>> operation,
            Supplier<T> onDuplicate) {

        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByKey(key);

        if (existing.isPresent()) {
            return onDuplicate.get();
        }

        IdempotencyResult<T> result = operation.get();
        persistKey(key, entityType, result.entityId(), result.httpStatus(), result.body());
        return result.body();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistKey(String key, String entityType, UUID entityId, int httpStatus, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            IdempotencyKey ik = IdempotencyKey.of(key, entityType, entityId, httpStatus, json);
            idempotencyKeyRepository.save(ik);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response for idempotency", e);
        }
    }

    public record IdempotencyResult<T>(T body, UUID entityId, int httpStatus) {}
}
