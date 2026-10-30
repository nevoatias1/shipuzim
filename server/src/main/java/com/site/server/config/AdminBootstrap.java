package com.site.server.config;

import com.site.server.model.CustomUser;
import com.site.server.model.Role;
import com.site.server.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserService users;
    private final Environment environment;

    public AdminBootstrap(UserService users, Environment environment) {
        this.users = users;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = environment.getProperty("ADMIN_USERNAME");
        String password = environment.getProperty("ADMIN_PASSWORD");
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)
                || users.getUserByUsername(username) != null) {
            return;
        }
        String firstName = environment.getProperty("ADMIN_FIRST_NAME");
        String lastName = environment.getProperty("ADMIN_LAST_NAME");
        String email = environment.getProperty("ADMIN_EMAIL");
        Assert.hasText(firstName, "ADMIN_FIRST_NAME is required when bootstrapping an admin");
        Assert.hasText(lastName, "ADMIN_LAST_NAME is required when bootstrapping an admin");
        Assert.hasText(email, "ADMIN_EMAIL is required when bootstrapping an admin");
        CustomUser admin = new CustomUser(firstName, lastName, email, null, null,
                username, password, Role.ADMIN);
        String result = users.register(admin);
        if (!"User registered successfully".equals(result)) {
            throw new IllegalStateException("Admin bootstrap failed; check the bootstrap settings and existing users");
        }
    }
}
