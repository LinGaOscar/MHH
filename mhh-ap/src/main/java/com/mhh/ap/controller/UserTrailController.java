package com.mhh.ap.controller;

import com.mhh.ap.annotation.LogAction;
import com.mhh.common.entity.UserLog;
import com.mhh.common.repository.UserLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-trail")
@RequiredArgsConstructor
public class UserTrailController {

    private final UserLogRepository userLogRepository;

    @GetMapping
    @LogAction(action = "VIEW_TRAIL", description = "查詢電磁軌跡")
    public Map<String, Object> query(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userIdParam = (userId != null && !userId.isBlank()) ? userId : null;
        String actionParam  = (action  != null && !action.isBlank())  ? action  : null;
        String statusParam  = (status  != null && !status.isBlank())  ? status  : null;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime from = (dateFrom != null && !dateFrom.isBlank()) ? LocalDateTime.parse(dateFrom, fmt) : null;
        LocalDateTime to   = (dateTo   != null && !dateTo.isBlank())   ? LocalDateTime.parse(dateTo,   fmt) : null;

        Page<UserLog> result = userLogRepository.findByFilters(
                userIdParam, actionParam, statusParam, from, to, PageRequest.of(page, size));

        List<Map<String, Object>> rows = result.getContent().stream().map(l -> {
            Map<String, Object> row = new HashMap<>();
            row.put("logId",       l.getLogId());
            row.put("userId",      l.getUserId());
            row.put("action",      l.getAction());
            row.put("description", l.getDescription());
            row.put("ipAddress",   l.getIpAddress());
            row.put("logTime",     l.getLogTime() != null ? l.getLogTime().toString().replace("T", " ") : "");
            row.put("status",      l.getStatus());
            return row;
        }).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("content",       rows);
        resp.put("totalElements", result.getTotalElements());
        resp.put("totalPages",    result.getTotalPages());
        resp.put("page",          result.getNumber());
        resp.put("size",          result.getSize());
        return resp;
    }
}
