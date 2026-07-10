package com.beril.mailnotifier.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;

@Component
public class GmailApiHealthIndicator implements HealthIndicator {

    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    @Override
    public Health health() {
        try {
            HttpURLConnection connection = (HttpURLConnection)
                    URI.create(GOOGLE_TOKEN_ENDPOINT).toURL().openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int status = connection.getResponseCode();
            // 405 (Method Not Allowed) beklenen yanıt — endpoint erişilebilir demek
            if (status < 500) {
                return Health.up()
                        .withDetail("endpoint", GOOGLE_TOKEN_ENDPOINT)
                        .withDetail("httpStatus", status)
                        .build();
            }
            return Health.down()
                    .withDetail("endpoint", GOOGLE_TOKEN_ENDPOINT)
                    .withDetail("httpStatus", status)
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("endpoint", GOOGLE_TOKEN_ENDPOINT)
                    .build();
        }
    }
}
