package com.site.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.Assert;

@Configuration
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpConfig {
    @Bean
    public JavaMailSender contactMailSender(@Value("${MAIL_HOST}") String host,
                                           @Value("${MAIL_PORT}") int port,
                                           @Value("${MAIL_USERNAME}") String username,
                                           @Value("${MAIL_PASSWORD}") String password) {
        Assert.hasText(host, "MAIL_HOST is required");
        Assert.isTrue(port > 0 && port <= 65535, "MAIL_PORT must be a valid port");
        Assert.hasText(username, "MAIL_USERNAME is required");
        Assert.hasText(password, "MAIL_PASSWORD is required");
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        var properties = sender.getJavaMailProperties();
        properties.setProperty("mail.debug", "false");
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.starttls.required", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "5000");
        properties.setProperty("mail.smtp.timeout", "5000");
        properties.setProperty("mail.smtp.writetimeout", "5000");
        return sender;
    }
}
