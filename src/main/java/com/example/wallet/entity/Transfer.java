package com.example.wallet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="transfers")
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter
    private Long fromAccountId;
    @Setter
    private Long toAccountId;
    @Setter
    private Long amount;
    private OffsetDateTime createdAt;

    public Transfer(Long fromAccountId, Long toAccountId, Long amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
