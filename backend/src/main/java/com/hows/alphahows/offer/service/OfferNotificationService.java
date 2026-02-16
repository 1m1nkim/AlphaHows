package com.hows.alphahows.offer.service;

import com.hows.alphahows.offer.dto.OfferNotificationMessage;
import com.hows.alphahows.offer.entity.Offer;
import com.hows.alphahows.user.entity.User;
import com.hows.alphahows.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public void notifyAdminsOfferCreated(Offer offer) {
        OfferNotificationMessage payload = new OfferNotificationMessage(
                "OFFER_CREATED",
                offer.getId(),
                "새 오퍼가 도착했습니다.",
                offer.getCompanyName() + " - " + offer.getPositionTitle(),
                LocalDateTime.now()
        );
        sendToAdmins(payload);
    }

    public void notifyRecruiterStatusChanged(Offer offer) {
        OfferNotificationMessage payload = new OfferNotificationMessage(
                "OFFER_STATUS_CHANGED",
                offer.getId(),
                "오퍼 상태가 변경되었습니다.",
                "현재 상태: " + offer.getStatus().name(),
                LocalDateTime.now()
        );
        String recruiterEmail = offer.getRecruiter().getEmail();
        if (recruiterEmail != null && !recruiterEmail.isBlank()) {
            sendToUserChannels(recruiterEmail, payload);
        }
    }

    public void notifyRecruiterOfferRead(Offer offer) {
        OfferNotificationMessage payload = new OfferNotificationMessage(
                "OFFER_READ_BY_ADMIN",
                offer.getId(),
                "관리자가 오퍼를 확인했습니다.",
                offer.getCompanyName() + " - " + offer.getPositionTitle(),
                LocalDateTime.now()
        );
        String recruiterEmail = offer.getRecruiter().getEmail();
        if (recruiterEmail != null && !recruiterEmail.isBlank()) {
            sendToUserChannels(recruiterEmail, payload);
        }
    }

    private void sendToAdmins(OfferNotificationMessage payload) {
        List<User> admins = userRepository.findByRoleIgnoreCase("ADMIN");
        for (User admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) {
                continue;
            }
            sendToUserChannels(admin.getEmail(), payload);
        }
    }

    private void sendToUserChannels(String email, OfferNotificationMessage payload) {
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload);
        messagingTemplate.convertAndSend("/topic/notifications/" + toTopicKey(email), payload);
    }

    private String toTopicKey(String email) {
        return email.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
