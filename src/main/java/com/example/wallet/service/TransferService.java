package com.example.wallet.service;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.TransferResponse;
import com.example.wallet.entity.Account;
import com.example.wallet.entity.IdempotencyKey;
import com.example.wallet.entity.Transfer;
import com.example.wallet.exception.*;
import com.example.wallet.repository.AccountRepository;
import com.example.wallet.repository.IdempotencyKeyRepository;
import com.example.wallet.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public TransferService(TransferRepository transferRepository,  AccountRepository accountRepository, IdempotencyKeyRepository idempotencyKeyRepository) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public TransferResponse transfer(TransferRequest transferRequest, String key){
        IdempotencyKey idempotencyKey = idempotencyKeyRepository.findById(key).orElse(null);
        if (idempotencyKey != null) {
            Long transferId = idempotencyKey.getTransferId();
            Transfer transfer = transferRepository.findById(transferId).orElseThrow(() -> new OrphanedIdempotencyKeyException(key, transferId));
            return TransferResponse.from(transfer);
        }

        if (transferRequest.fromAccountId().equals(transferRequest.toAccountId())) {
            throw new SameAccountTransferException();
        }

        Long firstId, secondId;

        if (transferRequest.fromAccountId() < transferRequest.toAccountId()) {
            firstId = transferRequest.fromAccountId();
            secondId = transferRequest.toAccountId();
        } else {
            firstId = transferRequest.toAccountId();
            secondId = transferRequest.fromAccountId();
        }
        Account account1 = accountRepository.findWithLockById(firstId).orElseThrow(() -> new AccountNotFoundException(firstId));
        Account account2 = accountRepository.findWithLockById(secondId).orElseThrow(() -> new AccountNotFoundException(secondId));

        try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        Account fromAccount, toAccount;

        if (account1.getId().equals(transferRequest.fromAccountId())) {
            fromAccount = account1;
            toAccount = account2;
        } else {
            fromAccount = account2;
            toAccount = account1;
        }

        if (fromAccount.getBalance() < transferRequest.amount()) {
            throw new InsufficientFundsException(transferRequest.amount(),  fromAccount.getBalance());
        }

        fromAccount.setBalance(fromAccount.getBalance() - transferRequest.amount());
        toAccount.setBalance(toAccount.getBalance() + transferRequest.amount());

        Transfer transfer = new Transfer(fromAccount.getId(), toAccount.getId(), transferRequest.amount());
        Transfer savedTransfer = transferRepository.save(transfer);

        IdempotencyKey newIdempotencyKey = new IdempotencyKey(key, savedTransfer.getId());
        idempotencyKeyRepository.save(newIdempotencyKey);

        return TransferResponse.from(savedTransfer);
    }
}
