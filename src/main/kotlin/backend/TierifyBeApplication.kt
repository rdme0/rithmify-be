package backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication @ConfigurationPropertiesScan class TierifyBeApplication

fun main(args: Array<String>) {
    runApplication<TierifyBeApplication>(*args)
}
