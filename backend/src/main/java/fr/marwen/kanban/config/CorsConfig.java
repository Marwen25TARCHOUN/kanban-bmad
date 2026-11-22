package fr.marwen.kanban.config;

import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Dev-mode CORS wiring (architecture AD-12) plus {@code JsonNullable} module registration (AD-13).
 *
 * <p><strong>CORS.</strong> In dev-mode the Angular dev server runs at {@code localhost:4200} and
 * the Spring Boot backend at {@code localhost:8080} — a two-origin setup. The hard-coded allow-list
 * here lets the browser's preflight pass. In containerized mode nginx reverse-proxies {@code /api}
 * to {@code backend:8080}, so the browser sees everything as same-origin and this bean's rules are
 * simply never exercised — no profile toggle needed (AD-12 "single codepath").
 *
 * <p><strong>Jackson module.</strong> {@code JsonNullableJackson3Module} teaches Jackson 3 how to
 * serialize/deserialize {@code JsonNullable<T>} on {@code PatchCardRequest}. Spring Boot 4.x ships
 * Jackson 3 ({@code tools.jackson.*}) as the primary HTTP message converter codec; its
 * auto-configuration scans for any {@code tools.jackson.databind.JacksonModule} bean and
 * registers it with the primary {@code ObjectMapper}. The {@code jackson-databind-nullable}
 * artifact 0.2.13+ ships both the Jackson 2 {@code JsonNullableModule} AND the Jackson 3
 * {@code JsonNullableJackson3Module}; we register the Jackson 3 one because that's what Spring
 * Boot 4.1 actually binds to {@code @RequestBody}.
 *
 * <p>Co-located here rather than in a dedicated class to keep the {@code config/} package minimal
 * for story 2 (Code Map lists only {@code CorsConfig}).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept-Language")
                .allowCredentials(false);
    }

    @Bean
    public JsonNullableJackson3Module jsonNullableJackson3Module() {
        return new JsonNullableJackson3Module();
    }
}
