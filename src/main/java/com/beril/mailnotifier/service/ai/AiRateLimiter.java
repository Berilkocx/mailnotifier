package com.beril.mailnotifier.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class AiRateLimiter {

    private final int maxPerMinute;
    private final int maxPerScan;

    private final AtomicInteger minuteCount = new AtomicInteger(0);
    private final AtomicInteger scanCount   = new AtomicInteger(0);

    public AiRateLimiter(
            @Value("${mailnotifier.ai.rate-limit.max-requests-per-minute:20}") int maxPerMinute,
            @Value("${mailnotifier.ai.rate-limit.max-analysis-per-scan:5}") int maxPerScan) {
        this.maxPerMinute = maxPerMinute;
        this.maxPerScan   = maxPerScan;
    }

    /** Dakikalık sayacı sıfırla. */
    @Scheduled(fixedRate = 60_000)
    public void resetMinuteCount() {
        int prev = minuteCount.getAndSet(0);
        if (prev > 0) {
            log.debug("AI dakika sayacı sıfırlandı: önceki={}", prev);
        }
    }

    /** Her kullanıcı tarama döngüsü başında çağrılır. */
    public void resetScanCount() {
        scanCount.set(0);
    }

    /**
     * Hem dakika hem de tarama limitini kontrol eder.
     * @return izin verilirse true
     */
    public boolean tryAcquire() {
        if (minuteCount.get() >= maxPerMinute) {
            log.debug("AI global rate limit aşıldı: {}/{} (dakika)", minuteCount.get(), maxPerMinute);
            return false;
        }
        if (scanCount.get() >= maxPerScan) {
            log.debug("AI scan rate limit aşıldı: {}/{} (tarama)", scanCount.get(), maxPerScan);
            return false;
        }
        minuteCount.incrementAndGet();
        scanCount.incrementAndGet();
        return true;
    }
}
