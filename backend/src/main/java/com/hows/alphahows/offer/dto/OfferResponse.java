package com.hows.alphahows.offer.dto;

import com.hows.alphahows.offer.entity.Offer;
import com.hows.alphahows.offer.entity.OfferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OfferResponse(
        Long offerId,
        String recruiterEmail,
        String companyName,
        String positionTitle,
        String contactEmail,
        String contactPhone,
        String employmentType,
        String workType,
        String message,
        OfferStatus status,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        String salaryUnit,
        LocalDateTime createdAt
) {
    public static OfferResponse from(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getRecruiter().getEmail(),
                offer.getCompanyName(),
                offer.getPositionTitle(),
                offer.getContactEmail(),
                offer.getContactPhone(),
                offer.getEmploymentType().name(),
                offer.getWorkType().name(),
                offer.getMessage(),
                offer.getStatus(),
                offer.getSalaryMin(),
                offer.getSalaryMax(),
                offer.getCurrency(),
                offer.getSalaryUnit() == null ? null : offer.getSalaryUnit().name(),
                offer.getCreatedAt()
        );
    }
}
