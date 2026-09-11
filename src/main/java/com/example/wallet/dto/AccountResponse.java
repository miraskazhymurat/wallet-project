package com.example.wallet.dto;

import com.example.wallet.entity.Account;

public record AccountResponse(Long id, String ownerName, Long balance) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getOwnerName(), account.getBalance());
    }
}
