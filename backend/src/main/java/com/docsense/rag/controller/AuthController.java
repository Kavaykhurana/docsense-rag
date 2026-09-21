package com.docsense.rag.controller;

import com.docsense.rag.dto.UserDto;
import com.docsense.rag.entity.User;
import com.docsense.rag.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication-related endpoints. Logout is handled by Spring Security's
 * logout filter configured in {@code SecurityConfig} (POST /api/auth/logout).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        return new UserDto(user.getId(), user.getEmail(), user.getName(), user.getProfilePicture());
    }
}
