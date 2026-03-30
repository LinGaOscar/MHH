package com.mhh.ap.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RouterController {

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("userName", "User");
        model.addAttribute("countryCode", "TW");
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "dashboard"; // Placeholder for dashboard.html
    }

    @GetMapping("/query")
    public String query(Model model) {
        return "query"; // Placeholder for query.html
    }
}
