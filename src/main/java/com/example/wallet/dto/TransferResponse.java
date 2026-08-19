package com.example.wallet.dto;

import com.example.wallet.entity.Transfer;

import java.time.OffsetDateTime;

public record TransferResponse(Long id, Long fromAccountId, Long toAccountId, Long amount, OffsetDateTime createdAt) {
    public static TransferResponse from(Transfer transfer){
        return new TransferResponse(transfer.getId(), transfer.getFromAccountId(), transfer.getToAccountId(), transfer.getAmount(), transfer.getCreatedAt());
    }
}
