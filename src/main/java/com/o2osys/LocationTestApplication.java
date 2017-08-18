package com.o2osys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Configuration
@PropertySource("classpath:config.properties")
public class LocationTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(LocationTestApplication.class, args);
	}
	
	@Bean(name = "messageSource")
	public ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageBundle = new ReloadableResourceBundleMessageSource();
		messageBundle.setBasename("classpath:messages/messages");
		messageBundle.setDefaultEncoding("UTF-8");
		messageBundle.setUseCodeAsDefaultMessage(true);
		return messageBundle;
	}
}
