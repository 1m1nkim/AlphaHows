package com.hows.alphahows.offer.dto;

import jakarta.validation.constraints.NotNull;

public record OfferReadUpdateRequest(
        @NotNull Boolean read
) {
}
