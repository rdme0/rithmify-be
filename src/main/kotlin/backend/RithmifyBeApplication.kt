package backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.web.config.EnableSpringDataWebSupport

// DB-backed tierlist/auth code is preserved, but PostgreSQL/JPA is disabled for the MVP.
@SpringBootApplication(
        exclude = [
                DataSourceAutoConfiguration::class,
                HibernateJpaAutoConfiguration::class,
                JpaRepositoriesAutoConfiguration::class
        ]
)
@ConfigurationPropertiesScan
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
class RithmifyBeApplication

fun main(args: Array<String>) {
    runApplication<RithmifyBeApplication>(*args)
}
