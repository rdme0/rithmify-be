
package backend.config

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration

@Configuration
class Resilience4jConfig {

    @Bean
    fun spotifyRateLimiter(): RateLimiter {
        val config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(30)) // 30초 윈도우
            .limitForPeriod(80) // 30초당 80개 요청 (안전 마진)
            .timeoutDuration(Duration.ofSeconds(5))
            .build()

        return RateLimiter.of("spotify-api", config)
    }

    @Bean
    fun spotifyRetry(): Retry {
        val config = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1000))
            .retryOnException { it is WebClientResponseException.TooManyRequests }
            .build()

        return Retry.of("spotify-retry", config)
    }
}
