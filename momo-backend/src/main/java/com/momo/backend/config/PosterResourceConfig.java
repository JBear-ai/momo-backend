package com.momo.backend.config;

import com.momo.backend.service.PosterFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class PosterResourceConfig implements WebMvcConfigurer {

    private final PosterFileStorageService posterFileStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/posters/**")
                .addResourceLocations(posterFileStorageService.location())
                .setCachePeriod(86400);
    }
}
