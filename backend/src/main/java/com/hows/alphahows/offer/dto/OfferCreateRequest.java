package com.hows.alphahows.offer.dto;

import com.hows.alphahows.offer.entity.EmploymentType;
import com.hows.alphahows.offer.entity.SalaryUnit;
import com.hows.alphahows.offer.entity.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OfferCreateRequest(
        @NotBlank String companyName,
        @NotBlank String positionTitle,
        String contactEmail,
        String contactPhone,
        @NotNull EmploymentType employmentType,
        @NotNull WorkType workType,
        String message,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        SalaryUnit salaryUnit
) {
}
