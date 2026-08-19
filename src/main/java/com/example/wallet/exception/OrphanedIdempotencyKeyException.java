package com.example.wallet.exception;

public class OrphanedIdempotencyKeyException extends RuntimeException {
    private String key;
    private Long transferId;
    public OrphanedIdempotencyKeyException(String key, Long transferId) {
        super("Idempotency key " + key + " references missing transfer " + transferId);
        this.key = key;
        this.transferId = transferId;
    }

    public String getKey() {
        return key;
    }

    public Long getTransferId() {
        return transferId;
    }
}
