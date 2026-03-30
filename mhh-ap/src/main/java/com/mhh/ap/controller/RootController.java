package com.mhh.ap.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @Value("${mhh.auth.dev-mode:false}")
    private boolean devMode;

    @GetMapping("/")
    public String index() {
        if (devMode) {
            // 開發模式：跳轉至 Spring Security 預設登入頁
            return "redirect:/login";
        } else {
            // 正式模式：跳轉至 SSO 驗證頁面 (目前為預留路徑)
            return "redirect:/sso/login";
        }
    }
}
