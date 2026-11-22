package com.vladyslavplus.lokora.reservation.scheduler;

import com.vladyslavplus.lokora.reservation.ReservationMaintenanceService;
import com.vladyslavplus.lokora.reservation.ResourceUtilizationDigest;
import com.vladyslavplus.lokora.reservation.ResourceUtilizationRow;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DailyResourceUtilizationDigestJob implements Job {

    static final String REPORT_WINDOW_DAYS_KEY = "reportWindowDays";
    private static final Logger logger = LoggerFactory.getLogger(DailyResourceUtilizationDigestJob.class);

    private final ReservationMaintenanceService maintenanceService;

    public DailyResourceUtilizationDigestJob(ReservationMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        TriggerKey triggerKey = context.getTrigger().getKey();
        int reportWindowDays = readReportWindowDays(context, jobKey, triggerKey);

        try {
            ResourceUtilizationDigest digest = maintenanceService.generateUtilizationDigest(reportWindowDays);
            logger.info("Resource utilization digest generated: jobKey={} triggerKey={} startDate={} "
                            + "endDateExclusive={} resourceCount={}",
                    jobKey, triggerKey, digest.startDate(), digest.endDateExclusive(), digest.resources().size());
            for (ResourceUtilizationRow row : digest.resources()) {
                logger.info("Resource utilization: resourceId={} reservationCount={} occupiedMinutes={}",
                        row.resourceId(), row.reservationCount(), row.occupiedMinutes());
            }
        } catch (RuntimeException failure) {
            logger.error("Resource utilization digest failed: jobKey={} triggerKey={} failureType={}",
                    jobKey, triggerKey, failure.getClass().getSimpleName());
            throw new JobExecutionException("Resource utilization digest failed", failure, false);
        }
    }

    private int readReportWindowDays(JobExecutionContext context, JobKey jobKey, TriggerKey triggerKey)
            throws JobExecutionException {
        String value = context.getMergedJobDataMap().getString(REPORT_WINDOW_DAYS_KEY);
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue > 0 && parsedValue <= ReservationMaintenanceService.MAX_REPORT_WINDOW_DAYS) {
                return parsedValue;
            }
        } catch (NumberFormatException | NullPointerException ignored) {
            // The Quartz configuration error below identifies the stable job/trigger keys without echoing bad data.
        }
        logger.error("Quartz job configuration is invalid: jobKey={} triggerKey={} missingOrInvalidKey={}",
                jobKey, triggerKey, REPORT_WINDOW_DAYS_KEY);
        throw new JobExecutionException("Invalid Quartz JobDataMap configuration", false);
    }
}
