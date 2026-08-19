package com.example.wallet.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(long amount, long balance) {

        super("Balance is not enough. Amount: " + amount + ", Balance: " + balance);
    }
}
