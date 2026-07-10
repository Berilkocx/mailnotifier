package com.beril.mailnotifier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires running PostgreSQL and GOOGLE_CLIENT_ID/SECRET environment variables")
class MailnotifierApplicationTests {

    @Test
    void contextLoads() {
    }
}
