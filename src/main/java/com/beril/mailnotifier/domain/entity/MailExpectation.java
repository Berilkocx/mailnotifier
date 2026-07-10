package com.beril.mailnotifier.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mail_expectations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailExpectation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sender_identifier", length = 500)
    private String senderIdentifier;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "mail_expectation_keywords",
            joinColumns = @JoinColumn(name = "expectation_id")
    )
    @Column(name = "keyword")
    private List<String> keywords;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
}
