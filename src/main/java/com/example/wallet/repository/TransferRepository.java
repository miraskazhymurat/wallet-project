package com.example.wallet.repository;

import com.example.wallet.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface    TransferRepository extends JpaRepository<Transfer, Long> {
    @Query("select t from Transfer t where t.fromAccountId = :accountId or t.toAccountId = :accountId")
    Page<Transfer> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
