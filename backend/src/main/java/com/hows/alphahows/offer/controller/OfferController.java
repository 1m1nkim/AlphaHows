package com.hows.alphahows.offer.controller;

import com.hows.alphahows.offer.dto.OfferCreateRequest;
import com.hows.alphahows.offer.dto.OfferResponse;
import com.hows.alphahows.offer.dto.OfferStatusUpdateRequest;
import com.hows.alphahows.offer.service.OfferService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    public OfferResponse createOffer(@Valid @RequestBody OfferCreateRequest request, Authentication authentication) {
        return offerService.createOffer(request, authentication);
    }

    @GetMapping
    public List<OfferResponse> getOffers(Authentication authentication) {
        return offerService.getOffers(authentication);
    }

    @GetMapping("/{offerId}")
    public OfferResponse getOffer(@PathVariable Long offerId, Authentication authentication) {
        return offerService.getOffer(offerId, authentication);
    }

    @PatchMapping("/{offerId}/status")
    public OfferResponse updateStatus(
            @PathVariable Long offerId,
            @Valid @RequestBody OfferStatusUpdateRequest request,
            Authentication authentication
    ) {
        return offerService.updateStatus(offerId, request, authentication);
    }
}
