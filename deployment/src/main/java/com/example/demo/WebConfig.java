package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer{
	@Override
	public void addResourceHandlers(final ResourceHandlerRegistry registry) {
	    registry.addResourceHandler("/**").
	    		 addResourceLocations("file:/home/share/matlab/MPSInstances/noHelmetPersonDetector/Step0_MyTestImages/");
	}
}
/*file path in Linux should be:/opt/x/y/z/static/*/