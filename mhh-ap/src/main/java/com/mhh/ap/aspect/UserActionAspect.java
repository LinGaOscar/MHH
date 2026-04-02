package com.mhh.ap.aspect;

import com.mhh.ap.annotation.LogAction;
import com.mhh.ap.service.UserLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 攔截標有 {@link LogAction} 的控制器方法，將結果記錄至 USER_LOGS。
 * 使用 SpEL 解析 description 模板（e.g. "查詢: #keyword"）。
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class UserActionAspect {

    private final UserLogService userLogService;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    @Around("@annotation(logAction)")
    public Object logUserAction(ProceedingJoinPoint joinPoint, LogAction logAction) throws Throwable {
        String userId = resolveUserId();
        String action = logAction.action();
        String description = resolveDescription(logAction.description(), joinPoint);

        try {
            Object result = joinPoint.proceed();
            userLogService.record(userId, action, description, "SUCCESS");
            return result;
        } catch (Throwable ex) {
            userLogService.record(userId, action, description + " | error=" + ex.getMessage(), "FAILED");
            throw ex;
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "ANONYMOUS";
    }

    private String resolveDescription(String template, ProceedingJoinPoint joinPoint) {
        if (template == null || template.isBlank()) return "";
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            Method method = sig.getMethod();
            Object[] args = joinPoint.getArgs();
            Parameter[] params = method.getParameters();

            EvaluationContext ctx = new StandardEvaluationContext();
            for (int i = 0; i < params.length; i++) {
                ctx.setVariable(params[i].getName(), args[i]);
            }
            String value = spelParser.parseExpression(template).getValue(ctx, String.class);
            return value != null ? value : template;
        } catch (Exception e) {
            return template;
        }
    }
}
