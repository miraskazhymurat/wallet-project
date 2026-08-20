package com.example.wallet.controller;

import com.example.wallet.dto.AccountRequest;
import com.example.wallet.dto.AccountResponse;
import com.example.wallet.dto.TransferResponse;
import com.example.wallet.service.AccountService;
import com.example.wallet.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;
    private final TransferService transferService;

    public AccountController(AccountService accountService, TransferService transferService) {
        this.accountService = accountService;
        this.transferService = transferService;
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable Long id){
        return accountService.getById(id);
    }

    @GetMapping("/{id}/transactions")
    public Page<TransferResponse> getTransactionsByAccountId(@PathVariable Long id, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        return transferService.getTransactionsByAccountId(id, pageable);
    }

    @PostMapping
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest accountRequest){
        return accountService.createAccount(accountRequest);
    }
}
