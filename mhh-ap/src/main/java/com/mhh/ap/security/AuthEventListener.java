package com.mhh.ap.security;

import com.mhh.ap.service.UserLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 監聽 Spring Security 的登入成功/失敗事件，寫入 USER_LOGS。
 * 登出透過 SecurityConfig 的 logoutSuccessHandler 處理。
 */
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final UserLogService userLogService;

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        String userId = event.getAuthentication().getName();
        String ip = resolveIp();
        userLogService.record(userId, "LOGIN", "登入成功 ip=" + ip, "SUCCESS");
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        String userId = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        String ip = resolveIp();
        userLogService.record(userId, "LOGIN", "登入失敗 ip=" + ip + " reason=" + reason, "FAILED");
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "UNKNOWN";
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
