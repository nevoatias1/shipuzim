package com.site.server.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.site.server.model.CustomUser;
import com.site.server.model.Role;
import com.site.server.service.UserService;
import com.site.server.utils.JwtUtil;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.google.client-id=stage7-test-client")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleSignInIntegrationTest {
    @MockitoBean GoogleIdTokenVerifier verifier;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserService users;
    @Autowired JwtUtil jwt;
    @Autowired JdbcTemplate jdbc;
    private static final AtomicInteger IPS = new AtomicInteger();
    private String ip;

    @BeforeEach
    void prepare() throws Exception {
        ip = "198.51.100." + IPS.incrementAndGet();
        jdbc.update("DELETE FROM users WHERE username IN (?, ?)", "google_stage7-sub", "stage7-existing");
        when(verifier.verify("synthetic-id-token")).thenReturn(token());
    }

    @Test
    void validTokenCreatesUserOnceWithBcryptAndReturnsUserJwt() throws Exception {
        var response = mapper.readTree(signIn().andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test Person"))
                .andReturn().getResponse().getContentAsString());
        String token = response.get("jwt").asText();
        assertThat(jwt.extractUsername(token)).isEqualTo("google_stage7-sub");
        assertThat(jwt.extractAuthorities(token)).extracting("authority").containsExactly("USER");
        CustomUser user = users.getUserByUsername("google_stage7-sub");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getEmail()).isEqualTo("stage7-google@example.test");
        assertThat(user.getPassword()).startsWith("$2");
        String password = user.getPassword();
        signIn().andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class,
                user.getUsername())).isEqualTo(1);
        assertThat(users.getUserByUsername(user.getUsername()).getPassword()).isEqualTo(password);
        mvc.perform(get("/admin/reviews").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void invalidOrMalformedTokenIsUnauthorizedWithoutCreatingUser() throws Exception {
        when(verifier.verify("synthetic-id-token")).thenReturn(null);
        signIn().andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("INVALID_GOOGLE_TOKEN"));
        when(verifier.verify("synthetic-id-token")).thenThrow(new IllegalArgumentException("sensitive token contents"));
        signIn().andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid Google ID token."));
        assertThat(users.getUserByUsername("google_stage7-sub")).isNull();
    }

    @Test
    void wrongAudienceAndUnverifiedEmailAreUnauthorized() throws Exception {
        GoogleIdToken token = token();
        token.getPayload().setAudience("different-client");
        when(verifier.verify("synthetic-id-token")).thenReturn(token);
        signIn().andExpect(status().isUnauthorized());
        token.getPayload().setAudience("stage7-test-client");
        token.getPayload().setEmailVerified(false);
        signIn().andExpect(status().isUnauthorized());
        assertThat(users.getUserByUsername("google_stage7-sub")).isNull();
    }

    @Test
    void wrongIssuerAndExpiredTokenAreUnauthorized() throws Exception {
        GoogleIdToken token = token();
        token.getPayload().setIssuer("https://untrusted.example.test");
        when(verifier.verify("synthetic-id-token")).thenReturn(token);
        signIn().andExpect(status().isUnauthorized());
        token.getPayload().setIssuer("accounts.google.com");
        token.getPayload().setExpirationTimeSeconds(Instant.now().getEpochSecond() - 1);
        signIn().andExpect(status().isUnauthorized());
    }

    @Test
    void emailConflictDoesNotLinkOrChangeAnExistingAccount() throws Exception {
        users.register(new CustomUser("Local", "Admin", "stage7-google@example.test", null, null,
                "stage7-existing", "synthetic-password", Role.ADMIN));
        signIn().andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("GOOGLE_ACCOUNT_CONFLICT"));
        assertThat(users.getUserByUsername("google_stage7-sub")).isNull();
        assertThat(users.getUserByUsername("stage7-existing").getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void tokenRoleClaimsAreIgnoredAndMissingNamesUseFallbacks() throws Exception {
        GoogleIdToken token = token();
        token.getPayload().set("role", "ADMIN");
        token.getPayload().set("roles", List.of("ADMIN"));
        token.getPayload().set("given_name", null);
        token.getPayload().set("family_name", " ");
        when(verifier.verify("synthetic-id-token")).thenReturn(token);
        String response = mvc.perform(post("/auth/google").with(request -> { request.setRemoteAddr(ip); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"synthetic-id-token\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("- -"))
                .andReturn().getResponse().getContentAsString();
        assertThat(jwt.extractAuthorities(mapper.readTree(response).get("jwt").asText()))
                .extracting("authority").containsExactly("USER");
        assertThat(users.getUserByUsername("google_stage7-sub").getRole()).isEqualTo(Role.USER);
    }

    @Test
    void existingNonUserGoogleUsernameCannotReceiveAUserOrAdminSession() throws Exception {
        users.register(new CustomUser("Local", "Admin", "stage7-google@example.test", null, null,
                "google_stage7-sub", "synthetic-password", Role.ADMIN));
        signIn().andExpect(status().isConflict());
    }

    @Test
    void googleAndPasswordAuthenticationShareTheExistingLimiter() throws Exception {
        when(verifier.verify("synthetic-id-token")).thenReturn(null);
        signIn().andExpect(status().isUnauthorized());
        signIn().andExpect(status().isUnauthorized());
        signIn().andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
        mvc.perform(post("/authenticate").with(request -> { request.setRemoteAddr(ip); return request; })
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests());
        verify(verifier, times(2)).verify("synthetic-id-token");
    }

    @Test
    void verifierIoFailureIsRedactedAndDoesNotCreateAnAccount() throws Exception {
        when(verifier.verify("synthetic-id-token")).thenThrow(new IOException("sensitive upstream details"));
        signIn().andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("INVALID_GOOGLE_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid Google ID token."));
        assertThat(users.getUserByUsername("google_stage7-sub")).isNull();
    }

    @Test
    void verifierConfigurationRestrictsAudienceIssuerAndExpiryWithoutNetwork() {
        GoogleIdTokenVerifier actual = new GoogleSignInConfig().googleIdTokenVerifier("expected-client");
        assertThat(actual.getAudience()).containsExactly("expected-client");
        assertThat(actual.getIssuers()).containsExactly("accounts.google.com", "https://accounts.google.com");
        assertThat(actual.getAcceptableTimeSkewSeconds()).isZero();
    }

    private ResultActions signIn() throws Exception {
        return mvc.perform(post("/auth/google").with(request -> { request.setRemoteAddr(ip); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("credential", "synthetic-id-token"))));
    }

    private GoogleIdToken token() {
        var payload = new GoogleIdToken.Payload();
        payload.setSubject("stage7-sub");
        payload.setAudience("stage7-test-client");
        payload.setIssuer("https://accounts.google.com");
        payload.setIssuedAtTimeSeconds(Instant.now().getEpochSecond() - 10);
        payload.setExpirationTimeSeconds(Instant.now().getEpochSecond() + 3600);
        payload.setEmail("stage7-google@example.test");
        payload.setEmailVerified(true);
        payload.set("given_name", "Test");
        payload.set("family_name", "Person");
        return new GoogleIdToken(new JsonWebSignature.Header(), payload,
                new byte[0], new byte[0]);
    }
}
