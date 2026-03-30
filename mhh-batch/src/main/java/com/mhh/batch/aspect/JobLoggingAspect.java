package com.mhh.batch.aspect;

import com.mhh.batch.job.MhhJob;
import com.mhh.batch.repository.JobConfRepository;
import com.mhh.batch.repository.JobLogRepository;
import com.mhh.common.entity.JobLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class JobLoggingAspect {

    private final JobLogRepository jobLogRepository;
    private final JobConfRepository jobConfRepository;

    public JobLoggingAspect(JobLogRepository jobLogRepository, JobConfRepository jobConfRepository) {
        this.jobLogRepository = jobLogRepository;
        this.jobConfRepository = jobConfRepository;
    }

    @Around("execution(* com.mhh.batch.job.MhhJob.execute(..))")
    @Transactional
    public Object profile(ProceedingJoinPoint joinPoint) throws Throwable {
        MhhJob job = (MhhJob) joinPoint.getTarget();
        String jobName = job.getJobName();
        LocalDateTime startTime = LocalDateTime.now();
        
        JobLog jobLog = JobLog.builder()
                .jobName(jobName)
                .startTime(startTime)
                .status("RUNNING")
                .build();
        
        jobLog = jobLogRepository.save(jobLog);

        try {
            Object result = joinPoint.proceed();
            
            jobLog.setEndTime(LocalDateTime.now());
            jobLog.setStatus("SUCCESS");
            
            return result;
        } catch (Throwable e) {
            jobLog.setEndTime(LocalDateTime.now());
            jobLog.setStatus("FAILED");
            jobLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            jobLogRepository.save(jobLog);
            // 更新 JOBS_CONF 的最後執行時間
            jobConfRepository.updateLastRun(jobName, startTime);
        }
    }
}
