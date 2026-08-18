package com.example.wallet.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(long id) {
        super("Account not found with id: " + id);
    }
}
