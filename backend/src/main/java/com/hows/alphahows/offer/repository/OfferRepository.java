package com.hows.alphahows.offer.repository;

import com.hows.alphahows.offer.entity.Offer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findByRecruiterIdOrderByIdDesc(Long recruiterId);

    Optional<Offer> findByIdAndRecruiterId(Long id, Long recruiterId);

    List<Offer> findAllByOrderByIdDesc();
}
