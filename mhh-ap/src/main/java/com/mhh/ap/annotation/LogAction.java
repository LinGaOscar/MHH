package com.mhh.ap.annotation;

import java.lang.annotation.*;

/**
 * 標記需要寫入 USER_LOGS 的控制器方法。
 *
 * <p>使用範例：
 * <pre>
 * {@literal @}LogAction(action = "SEARCH", description = "查詢電文")
 * public ResponseEntity<?> search(...) { ... }
 * </pre>
 *
 * @see com.mhh.ap.aspect.UserActionAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAction {

    /** 動作代碼，寫入 USER_LOGS.ACTION（e.g. LOGIN, SEARCH, RESERVE, DOWNLOAD） */
    String action();

    /** 描述模板，支援 SpEL 表達式取得方法參數（e.g. "查詢條件: #query"） */
    String description() default "";
}
