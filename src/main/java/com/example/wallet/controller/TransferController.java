package com.example.wallet.controller;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.TransferResponse;
import com.example.wallet.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService){
        this.transferService = transferService;
    }

    @PostMapping
    public TransferResponse transfer(@Valid @RequestBody TransferRequest transferRequest){
        return transferService.transfer(transferRequest);
    }
}
