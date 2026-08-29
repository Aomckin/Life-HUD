package io.github.aomckin.lifehud.config;

import io.github.aomckin.lifehud.repository.JsonRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Preserves the FastAPI /static mount used by the original web client. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final JsonRepository files;

    public WebConfig(JsonRepository files) { this.files = files; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(86_400);
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(files.resolve("uploads").toUri().toString())
                .setCachePeriod(86_400);
    }
}
