package com.mhh.core;

import org.springframework.stereotype.Service;

@Service
public class PlaceholderService {
    public String getStatus() {
        return "Core module is ready.";
    }
}
