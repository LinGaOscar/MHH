package com.mhh.batch;

import org.springframework.stereotype.Service;

@Service
public class BatchPlaceholderService {
    public String getStatus() {
        return "Batch module is ready.";
    }
}
