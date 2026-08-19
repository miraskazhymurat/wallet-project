package com.example.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(@NotNull Long fromAccountId, @NotNull Long toAccountId,
                              @NotNull @Positive Long amount) {
}
