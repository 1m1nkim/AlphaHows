package com.hows.alphahows.offer.dto;

import com.hows.alphahows.offer.entity.OfferStatus;
import jakarta.validation.constraints.NotNull;

public record OfferStatusUpdateRequest(
        @NotNull OfferStatus status
) {
}
