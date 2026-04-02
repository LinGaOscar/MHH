package com.mhh.batch.aspect;

import com.mhh.batch.job.MhhJob;
import com.mhh.batch.repository.JobConfRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class JobLoggingAspect {

    private static final Logger batchLog = LoggerFactory.getLogger("BATCH_LOG");

    private final JobConfRepository jobConfRepository;

    public JobLoggingAspect(JobConfRepository jobConfRepository) {
        this.jobConfRepository = jobConfRepository;
    }

    @Around("execution(* com.mhh.batch.job.MhhJob.execute(..))")
    public Object profile(ProceedingJoinPoint joinPoint) throws Throwable {
        MhhJob job = (MhhJob) joinPoint.getTarget();
        String jobName = job.getJobName();
        LocalDateTime startTime = LocalDateTime.now();

        batchLog.info("[{}] 開始執行 startTime={}", jobName, startTime);

        try {
            Object result = joinPoint.proceed();

            batchLog.info("[{}] 執行成功 startTime={} endTime={}", jobName, startTime, LocalDateTime.now());
            return result;
        } catch (Throwable e) {
            batchLog.error("[{}] 執行失敗 startTime={} endTime={} error={}", jobName, startTime, LocalDateTime.now(), e.getMessage());
            throw e;
        } finally {
            jobConfRepository.updateLastRun(jobName, startTime);
        }
    }
}
