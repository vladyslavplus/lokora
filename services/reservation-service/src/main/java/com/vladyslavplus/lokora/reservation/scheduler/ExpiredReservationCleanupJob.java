package com.vladyslavplus.lokora.reservation.scheduler;

import com.vladyslavplus.lokora.reservation.ReservationMaintenanceService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisallowConcurrentExecution
public class ExpiredReservationCleanupJob implements Job {

    static final String BATCH_SIZE_KEY = "batchSize";
    private static final Logger logger = LoggerFactory.getLogger(ExpiredReservationCleanupJob.class);

    private final ReservationMaintenanceService maintenanceService;

    public ExpiredReservationCleanupJob(ReservationMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        TriggerKey triggerKey = context.getTrigger().getKey();
        int batchSize = readPositiveInteger(context, BATCH_SIZE_KEY, jobKey, triggerKey);
        if (batchSize > ReservationMaintenanceService.MAX_CLEANUP_BATCH_SIZE) {
            throw configurationFailure(jobKey, triggerKey, BATCH_SIZE_KEY);
        }

        try {
            int completedCount = maintenanceService.completeExpiredReservations(batchSize);
            logger.info("Expired reservation cleanup finished: jobKey={} triggerKey={} completedCount={}",
                    jobKey, triggerKey, completedCount);
        } catch (RuntimeException failure) {
            logger.error("Expired reservation cleanup failed: jobKey={} triggerKey={} failureType={}",
                    jobKey, triggerKey, failure.getClass().getSimpleName());
            throw new JobExecutionException("Expired reservation cleanup failed", failure, false);
        }
    }

    private int readPositiveInteger(JobExecutionContext context, String key, JobKey jobKey, TriggerKey triggerKey)
            throws JobExecutionException {
        String value = context.getMergedJobDataMap().getString(key);
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue > 0) {
                return parsedValue;
            }
        } catch (NumberFormatException | NullPointerException ignored) {
            // The Quartz configuration error below identifies the stable job/trigger keys without echoing bad data.
        }
        throw configurationFailure(jobKey, triggerKey, key);
    }

    private JobExecutionException configurationFailure(JobKey jobKey, TriggerKey triggerKey, String key) {
        logger.error("Quartz job configuration is invalid: jobKey={} triggerKey={} missingOrInvalidKey={}",
                jobKey, triggerKey, key);
        return new JobExecutionException("Invalid Quartz JobDataMap configuration", false);
    }
}
