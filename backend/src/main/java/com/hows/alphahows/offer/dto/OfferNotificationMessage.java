package com.hows.alphahows.offer.dto;

import java.time.LocalDateTime;

public record OfferNotificationMessage(
        String type,
        Long offerId,
        String title,
        String message,
        LocalDateTime createdAt
) {
}
