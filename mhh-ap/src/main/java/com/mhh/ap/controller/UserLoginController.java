package com.mhh.ap.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserLoginController {

    @Value("${mhh.auth.dev-mode:false}")
    private boolean devMode;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"));
        response.put("username", auth != null ? auth.getName() : null);
        response.put("devMode", devMode);
        return response;
    }
}
