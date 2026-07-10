package com.beril.mailnotifier.domain.repository;

import com.beril.mailnotifier.domain.entity.MailExpectation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MailExpectationRepository extends JpaRepository<MailExpectation, UUID> {

    List<MailExpectation> findByUser_IdAndIsActiveTrue(UUID userId);

    List<MailExpectation> findAllByIsActiveTrue();

    List<MailExpectation> findByUser_Id(UUID userId);

    long countByUser_IdAndIsActiveTrue(UUID userId);

    Optional<MailExpectation> findByIdAndUser_Id(UUID id, UUID userId);

    boolean existsByIdAndUser_Id(UUID id, UUID userId);
}
