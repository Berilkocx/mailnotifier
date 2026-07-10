package com.beril.mailnotifier.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mail_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expectation_id", nullable = false)
    private MailExpectation expectation;

    // Composite unique kısıtı V3 migration'da tanımlandı: (gmail_message_id, expectation_id)
    @Column(name = "gmail_message_id", nullable = false)
    private String gmailMessageId;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "from_email")
    private String fromEmail;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String snippet;

    @Column(name = "match_score")
    private Double matchScore;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "mail_match_keywords",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "keyword")
    private List<String> matchedKeywords;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "mail_match_sender_tokens",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "token")
    private List<String> matchedSenderTokens;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false)
    @Builder.Default
    private ConfidenceLevel confidenceLevel = ConfidenceLevel.MEDIUM;

    @Column(name = "match_summary", columnDefinition = "TEXT")
    private String matchSummary;

    @Column(name = "content_summary", columnDefinition = "TEXT")
    private String contentSummary;

    @Column(name = "detected_intent", length = 50)
    private String detectedIntent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "mail_match_ai_topics",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "topic")
    private List<String> aiTopics;

    @Column(name = "dismissed", nullable = false)
    @Builder.Default
    private Boolean dismissed = false;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (dismissed == null) dismissed = false;
        if (confidenceLevel == null) confidenceLevel = ConfidenceLevel.MEDIUM;
    }
}
