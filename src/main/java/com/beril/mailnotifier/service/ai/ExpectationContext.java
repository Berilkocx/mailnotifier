package com.beril.mailnotifier.service.ai;

import java.util.List;

/** AI analizine, kullanıcının neyi beklediğini anlatan bağlam. */
public record ExpectationContext(String description, String senderIdentifier, List<String> keywords) {}
