package com.mhh.batch.service;

import com.mhh.batch.job.MhhJob;
import com.mhh.common.entity.JobConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class DynamicJobService {

    private final TaskScheduler taskScheduler;
    private final ApplicationContext context;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, String> currentCrons = new ConcurrentHashMap<>();

    public DynamicJobService(TaskScheduler taskScheduler, ApplicationContext context) {
        this.taskScheduler = taskScheduler;
        this.context = context;
    }

    /**
     * 註冊或更新任務
     */
    public void upsertJob(JobConf jobConf) {
        String jobName = jobConf.getJobName();
        String cron = jobConf.getFullCronExpression();
        boolean enabled = Boolean.TRUE.equals(jobConf.getIsEnabled());

        // 如果不啟用，則取消
        if (!enabled) {
            stopJob(jobName);
            return;
        }

        // 如果 Cron 沒變且正在跑，則跳過
        if (cron.equals(currentCrons.get(jobName)) && scheduledTasks.containsKey(jobName)) {
            return;
        }

        // 先停掉舊的
        stopJob(jobName);

        // 尋找對應的 Bean (根據 MhhJob.getJobName() 進行匹配)
        try {
            MhhJob jobBean = context.getBeansOfType(MhhJob.class).values().stream()
                    .filter(job -> jobName.equals(job.getJobName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("找不到對應的 Job 實作類"));

            log.info("正在啟動任務: {} [Cron: {}]", jobName, cron);
            
            ScheduledFuture<?> future = taskScheduler.schedule(jobBean::execute, new CronTrigger(cron));
            scheduledTasks.put(jobName, future);
            currentCrons.put(jobName, cron);
        } catch (Exception e) {
            log.error("無法啟動任務 {}, 錯誤: {}", jobName, e.getMessage());
        }
    }

    /**
     * 停止任務
     */
    public void stopJob(String jobName) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobName);
        if (future != null) {
            log.info("停止任務: {}", jobName);
            future.cancel(true);
        }
        currentCrons.remove(jobName);
    }
}
