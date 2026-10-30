package com.site.server.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.site.server.model.CustomUser;
import com.site.server.model.Role;
import com.site.server.service.UserService;
import com.site.server.utils.JwtUtil;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GoogleSignInService {
    private final GoogleIdTokenVerifier verifier;
    private final String clientId;
    private final UserService users;
    private final JwtUtil jwt;
    private final TransactionTemplate transactions;
    private final SecureRandom random = new SecureRandom();

    public GoogleSignInService(GoogleIdTokenVerifier verifier, @Value("${app.google.client-id}") String clientId,
                               UserService users, JwtUtil jwt, PlatformTransactionManager manager) {
        this.verifier = verifier;
        this.clientId = clientId;
        this.users = users;
        this.jwt = jwt;
        this.transactions = new TransactionTemplate(manager);
    }

    public SignInResponse signIn(String credential) {
        if (clientId.isBlank()) {
            throw new GoogleSignInException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_SIGN_IN_NOT_CONFIGURED",
                    "Google sign-in is not configured.");
        }
        GoogleIdToken token;
        try {
            token = verifier.verify(credential);
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw invalidToken();
        }
        if (token == null) throw invalidToken();
        GoogleIdToken.Payload payload = token.getPayload();
        // The official verifier checks these claims and the signature. Keep application requirements explicit.
        if (payload.getAudience() == null || !payload.getAudienceAsList().contains(clientId)
                || !("accounts.google.com".equals(payload.getIssuer())
                     || "https://accounts.google.com".equals(payload.getIssuer()))
                || payload.getExpirationTimeSeconds() == null
                || payload.getExpirationTimeSeconds() <= Instant.now().getEpochSecond()
                || !Boolean.TRUE.equals(payload.getEmailVerified())
                || payload.getSubject() == null || payload.getSubject().isBlank()
                || payload.getSubject().length() > 248
                || payload.getEmail() == null || payload.getEmail().isBlank() || payload.getEmail().length() > 255) {
            throw invalidToken();
        }
        String username = "google_" + payload.getSubject();
        CustomUser user;
        try {
            user = transactions.execute(status -> findOrCreate(username, payload));
        } catch (DuplicateKeyException race) {
            // A concurrent sign-in may have inserted the same identity. Re-read after the rollback.
            user = existingUser(username, payload.getEmail());
            if (user == null) throw conflict();
        }
        String tokenValue = jwt.generateToken(User.withUsername(user.getUsername()).password(user.getPassword())
                .authorities(Role.USER.name()).build());
        return new SignInResponse(tokenValue, user.getFirstName() + " " + user.getLastName());
    }

    private CustomUser findOrCreate(String username, GoogleIdToken.Payload payload) {
        CustomUser user = existingUser(username, payload.getEmail());
        if (user != null) return user;
        byte[] password = new byte[32];
        random.nextBytes(password);
        users.register(new CustomUser(name(payload.get("given_name")), name(payload.get("family_name")),
                payload.getEmail(), null, null, username,
                Base64.getUrlEncoder().withoutPadding().encodeToString(password), Role.USER));
        user = existingUser(username, payload.getEmail());
        if (user == null) throw conflict();
        return user;
    }

    private CustomUser existingUser(String username, String email) {
        CustomUser byEmail = users.getUserByEmail(email);
        if (byEmail != null && !username.equals(byEmail.getUsername())) throw conflict();
        CustomUser user = users.getUserByUsername(username);
        if (user != null && (user.getRole() != Role.USER || !email.equals(user.getEmail()))) throw conflict();
        return user;
    }

    private String name(Object claim) {
        if (!(claim instanceof String text) || text.isBlank()) return "-";
        String clean = text.replaceAll("[\\p{Cc}\\p{Cf}]", "").strip();
        if (clean.isEmpty()) return "-";
        return clean.substring(0, Math.min(255, clean.length()));
    }

    private GoogleSignInException invalidToken() {
        return new GoogleSignInException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid Google ID token.");
    }

    private GoogleSignInException conflict() {
        return new GoogleSignInException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_CONFLICT",
                "The email or Google identity is already linked to a different account.");
    }

    public record SignInResponse(String jwt, String displayName) {
        @Override public String toString() { return "SignInResponse[redacted]"; }
    }
}
