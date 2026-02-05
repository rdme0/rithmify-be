package backend.common.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

private val logger = KotlinLogging.logger {}

/**
 * Common Retry Utility based on Coroutines
 *
 * @param times 최대 재시도 횟수 (기본 3회)
 * @param initialDelay 초기 대기 시간 (ms)
 * @param factor 대기 시간 증가 배수 (Exponential Backoff)
 * @param retryCondition 재시도 여부를 결정하는 조건
 * @param extractDelay 예외로부터 특정 대기 시간을 추출하는 함수 (예: Retry-After 헤더). null 반환 시 initialDelay/factor 로직 따름
 */
suspend fun <T> retry(
    times: Int = 3,
    initialDelay: Long = 1000,
    factor: Double = 2.0,
    retryCondition: (Exception) -> Boolean = { true },
    extractDelay: (Exception) -> Long? = { null },
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (retryCondition(e)) {
                val specialDelay = extractDelay(e)
                val waitTime = specialDelay ?: currentDelay

                logger.warn { "Retrying... (${attempt + 1}/${times - 1}) error: ${e.message}. Waiting ${waitTime}ms" }
                delay(waitTime)

                // 특별한 대기 시간(Retry-After)이 없었을 때만 Backoff 적용
                if (specialDelay == null) {
                    currentDelay = (currentDelay * factor).toLong()
                }
            } else {
                throw e
            }
        }
    }
    return block() // Last attempt
}
