package com.site.server.config;

import com.site.server.gallery.storage.LocalImageStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "app.gallery.storage", havingValue = "local", matchIfMissing = true)
public class LocalUploadsConfig implements WebMvcConfigurer {
    private final LocalImageStorage storage;

    public LocalUploadsConfig(LocalImageStorage storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storage.getRoot().toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location)
                .setCacheControl(CacheControl.noCache());
    }
}
