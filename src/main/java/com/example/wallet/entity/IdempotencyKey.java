package com.example.wallet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id
    private String key;
    @Setter
    private Long transferId;
    private OffsetDateTime createdAt;

    public IdempotencyKey(String key, Long transferId) {
        this.key = key;
        this.transferId = transferId;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}