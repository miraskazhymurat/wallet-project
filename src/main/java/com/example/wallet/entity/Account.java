package com.example.wallet.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ownerName;
    private Long balance;
    private OffsetDateTime createdAt;

    public Account() {
    }

    public Account(String ownerName, Long balance) {
        this.ownerName = ownerName;
        this.balance = balance;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public OffsetDateTime getCreatedAt() {
        return this.createdAt;
    }
}
