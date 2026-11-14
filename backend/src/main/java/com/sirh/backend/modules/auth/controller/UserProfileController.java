package com.sirh.backend.modules.auth.controller;

import java.util.Map;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.auth.service.UserProfileService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profil")
public class UserProfileController {
    private final UserProfileService profiles;
    public UserProfileController(UserProfileService profiles) { this.profiles = profiles; }

    @GetMapping("/me")
    public Map<String, Object> profile(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return profiles.profile(principal.getId());
    }

    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> update(@AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam("fullName") String fullName, @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("region") String region, @RequestParam(value = "photo", required = false) MultipartFile photo) {
        return profiles.update(principal.getId(), fullName, phoneNumber, region, photo);
    }

    @PostMapping("/me/demande-verification")
    public Map<String, Object> requestVerification(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return profiles.requestVerification(principal.getId());
    }
}
