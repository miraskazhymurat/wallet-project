package com.example.wallet.controller;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.TransferResponse;
import com.example.wallet.service.TransferFacade;
import com.example.wallet.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transferService;
    private final TransferFacade  transferFacade;

    public TransferController(TransferService transferService, TransferFacade transferFacade) {
        this.transferService = transferService;
        this.transferFacade = transferFacade;
    }

    @PostMapping
    public TransferResponse transfer(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest transferRequest) {
        return transferFacade.transfer(transferRequest, key);
    }
}
