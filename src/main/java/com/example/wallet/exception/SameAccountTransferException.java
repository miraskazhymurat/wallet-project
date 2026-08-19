package com.example.wallet.exception;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Same account transfer is not acceptable");
    }
}
