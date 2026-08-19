package com.example.wallet.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(AccountNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException e){
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(SameAccountTransferException.class)
    public ResponseEntity<ErrorResponse> handleSameAccountTransferException(SameAccountTransferException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ErrorResponse> handleCannotAcquireLockException(CannotAcquireLockException e){
        log.error("Lock acquisition failed on transfer", e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Concurrent access to account, please retry"));
    }


    private record ErrorResponse(String message){}
}
