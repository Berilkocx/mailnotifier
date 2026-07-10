package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;

public interface MatchingStrategy {
    MatchResult match(MailMessage mail, MailExpectation expectation);
}
