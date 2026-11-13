package kr.sunsetcoast.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Sunsetcoast API")
                .description("선셋코스트 숙박 운영 데이터 플랫폼 API")
                .version("1.0"),
        )

    /**
     * springdoc이 버전 지정 API(`@GetMapping(version = ...)`)에 `X-API-Version` 헤더를 자동으로 붙이는데,
     * 기본값을 `1.0.0`으로 넣어 enum(`1.0`)과 어긋난다. [WebConfiguration]의 기본 버전에 맞춘다.
     */
    @Bean
    fun apiVersionHeaderCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.paths.orEmpty().values
            .flatMap { it.readOperations() }
            .flatMap { it.parameters.orEmpty() }
            .filter { it.`in` == "header" && it.name.equals(API_VERSION_HEADER, ignoreCase = true) }
            .forEach { param ->
                param.description = "API 버전. 생략하면 $DEFAULT_API_VERSION"
                @Suppress("UNCHECKED_CAST")
                (param.schema as io.swagger.v3.oas.models.media.Schema<Any>).setDefault(DEFAULT_API_VERSION)
            }
    }

    companion object {
        private const val API_VERSION_HEADER = "X-API-Version"
        private const val DEFAULT_API_VERSION = "1.0"
    }
}
