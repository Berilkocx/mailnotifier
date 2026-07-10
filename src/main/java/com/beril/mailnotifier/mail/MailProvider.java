package com.beril.mailnotifier.mail;

import com.beril.mailnotifier.domain.entity.User;

import java.time.Instant;
import java.util.List;

public interface MailProvider {

    List<MailMessage> fetchNewMessages(User user, Instant since);

    MailMessage getMessage(User user, String messageId);

    boolean testConnection(User user);
}
