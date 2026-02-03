package backend.config

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Resilience4jConfig {

    @Bean
    fun spotifyRateLimiter(): RateLimiter {
        val config =
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1)) // 1초 윈도우
                        .limitForPeriod(1) // 1초당 1개 요청 (매우 엄격)
                        .timeoutDuration(Duration.ofSeconds(60))
                        .build()

        return RateLimiter.of("spotify-api", config)
    }
}
