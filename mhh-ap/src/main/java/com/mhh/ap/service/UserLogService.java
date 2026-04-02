package com.mhh.ap.service;

import com.mhh.common.entity.UserLog;
import com.mhh.common.repository.UserLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLogService {

    private final UserLogRepository userLogRepository;

    /**
     * 非同步寫入 USER_LOGS，使用獨立 transaction 避免影響主流程。
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String userId, String action, String description, String status) {
        try {
            String ip = resolveClientIp();
            UserLog entry = UserLog.builder()
                    .userId(userId)
                    .action(action)
                    .description(description)
                    .ipAddress(ip)
                    .logTime(LocalDateTime.now())
                    .status(status)
                    .build();
            if (entry != null) userLogRepository.save(entry);
        } catch (Exception e) {
            // 日誌記錄不應讓主流程失敗
            log.error("[UserLogService] 寫入 USER_LOGS 失敗 userId={} action={} : {}", userId, action, e.getMessage());
        }
    }

    /** 取得客戶端真實 IP（考量反向代理） */
    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "SYSTEM";
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
