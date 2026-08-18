package com.example.wallet.service;

import com.example.wallet.dto.AccountRequest;
import com.example.wallet.dto.AccountResponse;
import com.example.wallet.entity.Account;
import com.example.wallet.exception.AccountNotFoundException;
import com.example.wallet.repository.AccountRepository;
import org.springframework.stereotype.Service;


@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse getById(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        return AccountResponse.from(account);
    }

    public AccountResponse createAccount(AccountRequest accountRequest) {
        Account account = accountRepository.save(new Account(accountRequest.ownerName(), accountRequest.balance()));
        return AccountResponse.from(account);
    }

}
