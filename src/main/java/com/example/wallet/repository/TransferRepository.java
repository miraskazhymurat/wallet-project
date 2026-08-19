package com.example.wallet.repository;

import com.example.wallet.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
