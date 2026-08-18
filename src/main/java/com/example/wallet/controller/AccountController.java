package com.example.wallet.controller;

import com.example.wallet.dto.AccountRequest;
import com.example.wallet.dto.AccountResponse;
import com.example.wallet.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService){
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable Long id){
        return accountService.getById(id);
    }

    @PostMapping
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest accountRequest){
        return accountService.createAccount(accountRequest);
    }
}
