package com.beril.mailnotifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Yeni mail beklentisi oluşturma isteği")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpectationRequest {

    @Schema(description = "Gönderen kriteri: tam email, domain, ad, soyad veya kurum adı",
            example = "ahmet@firma.com")
    private String senderIdentifier;

    @Schema(description = "Mail konusu ve içeriğinde aranacak anahtar kelimeler",
            example = "[\"teklif\", \"fatura\", \"başvuru\"]")
    private List<String> keywords;

    @Schema(description = "Kişisel hatırlatma notu (opsiyonel)", example = "İşe alım teklifi")
    private String description;
}
