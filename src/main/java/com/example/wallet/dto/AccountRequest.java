package com.example.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AccountRequest(@NotBlank String ownerName, @NotNull @PositiveOrZero Long balance) {

}
