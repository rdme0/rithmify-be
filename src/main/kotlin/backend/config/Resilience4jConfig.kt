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
                        .limitRefreshPeriod(Duration.ofSeconds(30)) // 30초 윈도우
                        .limitForPeriod(80) // 30초당 80개 요청 (안전 마진)
                        .timeoutDuration(Duration.ofSeconds(60))
                        .build()

        return RateLimiter.of("spotify-api", config)
    }
}
