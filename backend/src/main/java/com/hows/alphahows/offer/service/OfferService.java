package com.hows.alphahows.offer.service;

import com.hows.alphahows.auth.util.AuthPrincipalUtils;
import com.hows.alphahows.offer.dto.OfferCreateRequest;
import com.hows.alphahows.offer.dto.OfferConfirmResponse;
import com.hows.alphahows.offer.dto.OfferReadUpdateRequest;
import com.hows.alphahows.offer.dto.OfferResponse;
import com.hows.alphahows.offer.dto.OfferStatusUpdateRequest;
import com.hows.alphahows.offer.dto.OfferUnreadCountResponse;
import com.hows.alphahows.offer.entity.Offer;
import com.hows.alphahows.offer.entity.OfferStatus;
import com.hows.alphahows.offer.repository.OfferRepository;
import com.hows.alphahows.user.entity.User;
import com.hows.alphahows.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final OfferNotificationService offerNotificationService;

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
        offer.markReadByAdmin(false);
        offer.markReadByRecruiter(true);

        Offer savedOffer = offerRepository.save(offer);
        offerNotificationService.notifyAdminsOfferCreated(savedOffer);
        return OfferResponse.from(savedOffer, isReadForUser(savedOffer, requester), savedOffer.isAdminRead());
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getOffers(
            Authentication authentication,
            OfferStatus status,
            Boolean read,
            String keyword
    ) {
        User requester = resolveCurrentUser(authentication);
        boolean admin = isAdmin(requester);

        List<Offer> offers = admin
                ? offerRepository.findAllByOrderByIdDesc()
                : offerRepository.findByRecruiterIdOrderByIdDesc(requester.getId());

        return offers.stream()
                .filter(offer -> status == null || offer.getStatus() == status)
                .filter(offer -> read == null || isReadFilterMatched(offer, admin, read))
                .filter(offer -> matchesKeyword(offer, keyword, admin))
                .map(offer -> OfferResponse.from(offer, isReadForUser(offer, requester), offer.isAdminRead()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getOffer(Long offerId, Authentication authentication) {
        User requester = resolveCurrentUser(authentication);

        Offer offer = findReadableOffer(offerId, requester);
        return OfferResponse.from(offer, isReadForUser(offer, requester), offer.isAdminRead());
    }

    @Transactional
    public OfferResponse updateStatus(Long offerId, OfferStatusUpdateRequest request, Authentication authentication) {
        User requester = resolveCurrentUser(authentication);
        Offer offer = findWritableOffer(offerId, requester);

        validateStatusTransition(offer.getStatus(), request.status());
        offer.updateStatus(request.status());
        if (isAdmin(requester)) {
            offer.markReadByAdmin(true);
            offer.markReadByRecruiter(false);
            offerNotificationService.notifyRecruiterStatusChanged(offer);
        }

        return OfferResponse.from(offer, isReadForUser(offer, requester), offer.isAdminRead());
    }

    @Transactional
    public OfferResponse updateRead(Long offerId, OfferReadUpdateRequest request, Authentication authentication) {
        User requester = resolveCurrentUser(authentication);
        Offer offer = findReadableOffer(offerId, requester);

        if (!isAdmin(requester)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can update read state");
        }

        boolean before = offer.isAdminRead();
        boolean after = Boolean.TRUE.equals(request.read());
        offer.markReadByAdmin(after);

        if (!before && after) {
            offer.markReadByRecruiter(false);
            offerNotificationService.notifyRecruiterOfferRead(offer);
        }

        return OfferResponse.from(offer, isReadForUser(offer, requester), offer.isAdminRead());
    }

    @Transactional(readOnly = true)
    public OfferUnreadCountResponse getUnreadCount(Authentication authentication) {
        User requester = resolveCurrentUser(authentication);
        long unreadCount = isAdmin(requester)
                ? offerRepository.countByAdminReadFalse()
                : offerRepository.countByRecruiterIdAndRecruiterReadFalse(requester.getId());
        return new OfferUnreadCountResponse(unreadCount);
    }

    @Transactional
    public OfferConfirmResponse confirmUnreadForUser(Authentication authentication) {
        User requester = resolveCurrentUser(authentication);
        if (isAdmin(requester)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin does not use confirmUnreadForUser");
        }

        List<Offer> unreadOffers = offerRepository.findByRecruiterIdAndRecruiterReadFalseOrderByIdDesc(requester.getId());
        for (Offer offer : unreadOffers) {
            offer.markReadByRecruiter(true);
        }

        return new OfferConfirmResponse(unreadOffers.size());
    }

    @Transactional
    public OfferResponse confirmOfferForUser(Long offerId, Authentication authentication) {
        User requester = resolveCurrentUser(authentication);
        if (isAdmin(requester)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin does not use confirmOfferForUser");
        }

        Offer offer = offerRepository.findByIdAndRecruiterId(offerId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));

        offer.markReadByRecruiter(true);
        return OfferResponse.from(offer, isReadForUser(offer, requester), offer.isAdminRead());
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

        String email = AuthPrincipalUtils.resolveEmail(authentication);

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot resolve current user");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private boolean isReadForUser(Offer offer, User requester) {
        return isAdmin(requester) ? offer.isAdminRead() : offer.isRecruiterRead();
    }

    private boolean isReadFilterMatched(Offer offer, boolean admin, boolean read) {
        if (admin) {
            return offer.isAdminRead() == read;
        }
        return offer.isAdminRead() == read;
    }

    private boolean matchesKeyword(Offer offer, String keyword, boolean admin) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(offer.getCompanyName(), query)
                || contains(offer.getPositionTitle(), query)
                || contains(offer.getMessage(), query)
                || (admin && contains(offer.getRecruiter().getEmail(), query));
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
