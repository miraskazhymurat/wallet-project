package com.example.wallet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name="idempotency_keys")
public class IdempotencyKey {
    @Id
    private String key;
    private Long transferId;
    private OffsetDateTime createdAt;

    public IdempotencyKey() {}
    public IdempotencyKey(String key, Long transferId) {
        this.key = key;
        this.transferId = transferId;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public String getKey() {
        return key;
    }

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}