package com.site.server.controller;

import com.site.server.google.GoogleSignInService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleAuthenticationController {
    private final GoogleSignInService service;

    public GoogleAuthenticationController(GoogleSignInService service) { this.service = service; }

    @PostMapping("/auth/google")
    public GoogleSignInService.SignInResponse signIn(@Valid @RequestBody GoogleCredential request) {
        return service.signIn(request.credential());
    }

    public record GoogleCredential(@NotBlank @Size(max = 16384) String credential) {
        @Override public String toString() { return "GoogleCredential[redacted]"; }
    }
}
