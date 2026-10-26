package com.vladyslavplus.lokora.reservation.scheduler;

import com.vladyslavplus.lokora.reservation.QuartzTimeZoneResolver;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import java.util.TimeZone;

@Configuration
public class QuartzScheduleConfiguration {

    static final String RESERVATION_MAINTENANCE_GROUP = "reservation-maintenance";
    static final String RESERVATION_REPORTING_GROUP = "reservation-reporting";
    static final JobKey CLEANUP_JOB_KEY = new JobKey("expired-reservation-cleanup", RESERVATION_MAINTENANCE_GROUP);
    static final JobKey DIGEST_JOB_KEY = new JobKey("daily-resource-utilization-digest", RESERVATION_REPORTING_GROUP);

    private final TimeZone scheduleTimeZone;

    public QuartzScheduleConfiguration(
            QuartzTimeZoneResolver timeZoneResolver,
            @Value("${lokora.quartz.time-zone:Europe/Kyiv}") String configuredTimeZone
    ) {
        this.scheduleTimeZone = TimeZone.getTimeZone(timeZoneResolver.resolve(configuredTimeZone));
    }

    @Bean
    JobDetail expiredReservationCleanupJobDetail() {
        return JobBuilder.newJob(ExpiredReservationCleanupJob.class)
                .withIdentity(CLEANUP_JOB_KEY)
                .withDescription("Complete active reservations that have ended")
                .usingJobData(ExpiredReservationCleanupJob.BATCH_SIZE_KEY, "100")
                .storeDurably()
                .build();
    }

    @Bean
    JobDetail dailyResourceUtilizationDigestJobDetail() {
        return JobBuilder.newJob(DailyResourceUtilizationDigestJob.class)
                .withIdentity(DIGEST_JOB_KEY)
                .withDescription("Summarize prior-day reservation utilization by resource")
                .usingJobData(DailyResourceUtilizationDigestJob.REPORT_WINDOW_DAYS_KEY, "1")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger frequentExpiredReservationCleanupTrigger(JobDetail expiredReservationCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("frequent-sweep", RESERVATION_MAINTENANCE_GROUP)
                .forJob(expiredReservationCleanupJobDetail)
                .usingJobData(ExpiredReservationCleanupJob.BATCH_SIZE_KEY, "50")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(5)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    @Bean
    Trigger nightlyExpiredReservationCleanupTrigger(JobDetail expiredReservationCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("nightly-catch-up", RESERVATION_MAINTENANCE_GROUP)
                .forJob(expiredReservationCleanupJobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?")
                        .inTimeZone(scheduleTimeZone)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    Trigger dailyResourceUtilizationDigestTrigger(JobDetail dailyResourceUtilizationDigestJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("daily-digest", RESERVATION_REPORTING_GROUP)
                .forJob(dailyResourceUtilizationDigestJobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 6 * * ?")
                        .inTimeZone(scheduleTimeZone)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    SchedulerFactoryBeanCustomizer springQuartzJobFactory(AutowireCapableBeanFactory beanFactory) {
        return schedulerFactory -> schedulerFactory.setJobFactory(new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(TriggerFiredBundle bundle) {
                return beanFactory.createBean(bundle.getJobDetail().getJobClass());
            }
        });
    }
}
