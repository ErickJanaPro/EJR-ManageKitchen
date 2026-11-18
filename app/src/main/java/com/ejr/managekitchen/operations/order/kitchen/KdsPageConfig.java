package com.ejr.managekitchen.operations.order.kitchen;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sert la page de l'écran cuisine sur /kds et /kds/ (fichier static/kds/index.html). */
@Configuration
public class KdsPageConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/kds").setViewName("redirect:/kds/");
        registry.addViewController("/kds/").setViewName("forward:/kds/index.html");
    }
}
