package com.yusufziyrek.order.presentation.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeOrderStatusRequest(
        @NotBlank String status
) {
}
