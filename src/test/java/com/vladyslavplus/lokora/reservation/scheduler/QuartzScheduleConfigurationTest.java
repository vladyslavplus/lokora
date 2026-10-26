package com.vladyslavplus.lokora.reservation.scheduler;

import com.vladyslavplus.lokora.reservation.QuartzTimeZoneResolver;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzScheduleConfigurationTest {

    private final QuartzScheduleConfiguration configuration =
            new QuartzScheduleConfiguration(new QuartzTimeZoneResolver(), "Europe/Kyiv");

    @Test
    void cleanupJobHasStableIdentityAndDefaultBatchSize() {
        JobDetail job = configuration.expiredReservationCleanupJobDetail();

        assertThat(job.getKey()).isEqualTo(QuartzScheduleConfiguration.CLEANUP_JOB_KEY);
        assertThat(job.getJobDataMap().getString("batchSize")).isEqualTo("100");
        assertThat(job.isDurable()).isTrue();
    }

    @Test
    void cleanupTriggersShareJobButHaveDistinctKeysOverridesAndMisfirePolicies() {
        JobDetail job = configuration.expiredReservationCleanupJobDetail();
        Trigger frequent = configuration.frequentExpiredReservationCleanupTrigger(job);
        Trigger nightly = configuration.nightlyExpiredReservationCleanupTrigger(job);

        assertThat(frequent.getJobKey()).isEqualTo(job.getKey());
        assertThat(frequent.getKey().getName()).isEqualTo("frequent-sweep");
        assertThat(frequent.getKey().getGroup()).isEqualTo("reservation-maintenance");
        assertThat(frequent.getJobDataMap().getString("batchSize")).isEqualTo("50");
        assertThat(((SimpleTrigger) frequent).getRepeatInterval()).isEqualTo(300_000L);
        assertThat(frequent.getMisfireInstruction())
                .isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);

        assertThat(nightly.getJobKey()).isEqualTo(job.getKey());
        assertThat(nightly.getKey().getName()).isEqualTo("nightly-catch-up");
        assertThat(nightly.getKey().getGroup()).isEqualTo("reservation-maintenance");
        assertThat(nightly.getJobDataMap().containsKey("batchSize")).isFalse();
        assertThat(((CronTrigger) nightly).getCronExpression()).isEqualTo("0 0 2 * * ?");
        assertThat(((CronTrigger) nightly).getTimeZone().getID()).isEqualTo("Europe/Kyiv");
        assertThat(nightly.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
    }

    @Test
    void reportJobHasDailyKyivCronAndExplicitMisfireBehavior() {
        JobDetail job = configuration.dailyResourceUtilizationDigestJobDetail();
        Trigger trigger = configuration.dailyResourceUtilizationDigestTrigger(job);

        assertThat(job.getKey()).isEqualTo(QuartzScheduleConfiguration.DIGEST_JOB_KEY);
        assertThat(job.getJobDataMap().getString("reportWindowDays")).isEqualTo("1");
        assertThat(trigger.getJobKey()).isEqualTo(job.getKey());
        assertThat(trigger.getKey().getName()).isEqualTo("daily-digest");
        assertThat(((CronTrigger) trigger).getCronExpression()).isEqualTo("0 0 6 * * ?");
        assertThat(((CronTrigger) trigger).getTimeZone()).isEqualTo(TimeZone.getTimeZone("Europe/Kyiv"));
        assertThat(trigger.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
    }

    @Test
    void cleanupJobDisallowsOverlappingExecutions() {
        assertThat(ExpiredReservationCleanupJob.class)
                .hasAnnotation(org.quartz.DisallowConcurrentExecution.class);
    }

    @Test
    void unknownConfiguredZoneUsesGmtForCronTriggers() {
        QuartzScheduleConfiguration invalidZoneConfiguration =
                new QuartzScheduleConfiguration(new QuartzTimeZoneResolver(), "Invalid/Unknown");

        CronTrigger trigger = (CronTrigger) invalidZoneConfiguration.nightlyExpiredReservationCleanupTrigger(
                invalidZoneConfiguration.expiredReservationCleanupJobDetail()
        );

        assertThat(trigger.getTimeZone().getID()).isEqualTo("GMT");
    }
}
