package com.example.wallet.service;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.TransferResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class TransferFacade {
    private final TransferService transferService;

    public TransferFacade(TransferService transferService) {
        this.transferService = transferService;
    }

    public TransferResponse transfer(TransferRequest transferRequest, String key) {
        try {
            return transferService.executeTransfer(transferRequest, key);
        } catch (DataIntegrityViolationException e) {
            return transferService.findByIdempotencyKey(key);
        }
    }
}
