package com.example.wallet.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String key) {
        super("Idempotency conflict: key " + key + " used for other transfer");
    }
}
