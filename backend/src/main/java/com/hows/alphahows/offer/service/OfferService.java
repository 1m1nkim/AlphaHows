package com.hows.alphahows.offer.service;

import com.hows.alphahows.offer.dto.OfferCreateRequest;
import com.hows.alphahows.offer.dto.OfferResponse;
import com.hows.alphahows.offer.dto.OfferStatusUpdateRequest;
import com.hows.alphahows.offer.entity.Offer;
import com.hows.alphahows.offer.entity.OfferStatus;
import com.hows.alphahows.offer.repository.OfferRepository;
import com.hows.alphahows.user.entity.User;
import com.hows.alphahows.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;

    @Transactional
    public OfferResponse createOffer(OfferCreateRequest request, Authentication authentication) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());
        User requester = resolveCurrentUser(authentication);

        Offer offer = Offer.builder()
                .recruiter(requester)
                .companyName(request.companyName())
                .positionTitle(request.positionTitle())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .employmentType(request.employmentType())
                .workType(request.workType())
                .message(request.message())
                .status(OfferStatus.SUBMITTED)
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .currency(normalizeCurrency(request.currency()))
                .salaryUnit(request.salaryUnit())
                .build();

        return OfferResponse.from(offerRepository.save(offer));
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getOffers(Authentication authentication) {
        User requester = resolveCurrentUser(authentication);

        if (isAdmin(requester)) {
            return offerRepository.findAllByOrderByIdDesc().stream()
                    .map(OfferResponse::from)
                    .toList();
        }

        return offerRepository.findByRecruiterIdOrderByIdDesc(requester.getId()).stream()
                .map(OfferResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getOffer(Long offerId, Authentication authentication) {
        User requester = resolveCurrentUser(authentication);

        Offer offer = findReadableOffer(offerId, requester);
        return OfferResponse.from(offer);
    }

    @Transactional
    public OfferResponse updateStatus(Long offerId, OfferStatusUpdateRequest request, Authentication authentication) {
        User requester = resolveCurrentUser(authentication);
        Offer offer = findWritableOffer(offerId, requester);

        validateStatusTransition(offer.getStatus(), request.status());
        offer.updateStatus(request.status());

        return OfferResponse.from(offer);
    }

    private Offer findReadableOffer(Long offerId, User requester) {
        if (isAdmin(requester)) {
            return offerRepository.findById(offerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        }

        return offerRepository.findByIdAndRecruiterId(offerId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
    }

    private Offer findWritableOffer(Long offerId, User requester) {
        return findReadableOffer(offerId, requester);
    }

    private void validateSalaryRange(java.math.BigDecimal salaryMin, java.math.BigDecimal salaryMax) {
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "salaryMin cannot be greater than salaryMax");
        }
    }

    private void validateStatusTransition(OfferStatus current, OfferStatus target) {
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (current == target) {
            return;
        }
        if (current == OfferStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closed offer cannot be changed");
        }
        if (target.ordinal() < current.ordinal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move status backward");
        }
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase();
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Object principal = authentication.getPrincipal();
        String email = null;

        if (principal instanceof String username) {
            email = username;
        } else if (principal instanceof OAuth2User oauth2User) {
            email = extractKakaoEmail(oauth2User);
        }

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot resolve current user");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @SuppressWarnings("unchecked")
    private String extractKakaoEmail(OAuth2User oauth2User) {
        Object kakaoAccountObj = oauth2User.getAttributes().get("kakao_account");
        if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
            Object emailObj = kakaoAccount.get("email");
            if (emailObj != null) {
                return emailObj.toString();
            }
        }

        Object idObj = oauth2User.getAttributes().get("id");
        if (idObj != null) {
            return idObj + "@kakao.com";
        }

        return Optional.ofNullable(oauth2User.getName())
                .map(name -> name + "@kakao.com")
                .orElse(null);
    }
}
