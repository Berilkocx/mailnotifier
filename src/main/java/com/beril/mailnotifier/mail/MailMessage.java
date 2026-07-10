package com.beril.mailnotifier.mail;

import java.time.Instant;

public record MailMessage(
        String messageId,
        String from,
        String fromEmail,
        String subject,
        String snippet,
        String body,
        Instant receivedAt
) {}
