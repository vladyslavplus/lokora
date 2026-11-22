package com.vladyslavplus.lokora.reservation.scheduler;

import com.vladyslavplus.lokora.reservation.ReservationMaintenanceService;
import com.vladyslavplus.lokora.reservation.ResourceUtilizationDigest;
import com.vladyslavplus.lokora.reservation.ResourceUtilizationRow;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QuartzJobTest {

    @Test
    void cleanupJobDelegatesOneCallUsingMergedJobDataMap() throws Exception {
        ReservationMaintenanceService service = mock(ReservationMaintenanceService.class);
        QuartzScheduleConfiguration configuration =
                new QuartzScheduleConfiguration(new com.vladyslavplus.lokora.reservation.QuartzTimeZoneResolver(),
                        "Europe/Kyiv");
        JobDetail jobDetail = configuration.expiredReservationCleanupJobDetail();
        OperableTrigger trigger = (OperableTrigger) configuration.frequentExpiredReservationCleanupTrigger(jobDetail);
        Date firedTime = new Date();
        TriggerFiredBundle firedBundle = new TriggerFiredBundle(
                jobDetail, trigger, null, false, firedTime, firedTime, null, null
        );
        JobExecutionContext context = new JobExecutionContextImpl(
                null, firedBundle, new ExpiredReservationCleanupJob(service)
        );
        when(service.completeExpiredReservations(50)).thenReturn(2);

        new ExpiredReservationCleanupJob(service).execute(context);

        assertThat(context.getMergedJobDataMap().getString("batchSize")).isEqualTo("50");
        verify(service).completeExpiredReservations(50);
    }

    @Test
    void cleanupTriggerWithoutOverrideInheritsJobDetailDefault() throws Exception {
        ReservationMaintenanceService service = mock(ReservationMaintenanceService.class);
        QuartzScheduleConfiguration configuration =
                new QuartzScheduleConfiguration(new com.vladyslavplus.lokora.reservation.QuartzTimeZoneResolver(),
                        "Europe/Kyiv");
        JobDetail jobDetail = configuration.expiredReservationCleanupJobDetail();
        OperableTrigger trigger = (OperableTrigger) configuration.nightlyExpiredReservationCleanupTrigger(jobDetail);
        Date firedTime = new Date();
        TriggerFiredBundle firedBundle = new TriggerFiredBundle(
                jobDetail, trigger, null, false, firedTime, firedTime, null, null
        );
        JobExecutionContext context = new JobExecutionContextImpl(
                null, firedBundle, new ExpiredReservationCleanupJob(service)
        );

        new ExpiredReservationCleanupJob(service).execute(context);

        assertThat(context.getMergedJobDataMap().getString("batchSize")).isEqualTo("100");
        verify(service).completeExpiredReservations(100);
    }

    @Test
    void cleanupJobRejectsMissingOrInvalidDataWithoutImmediateRefire() {
        ReservationMaintenanceService service = mock(ReservationMaintenanceService.class);
        JobExecutionContext context = context("expired-reservation-cleanup", "nightly-catch-up", new JobDataMap());

        assertThatThrownBy(() -> new ExpiredReservationCleanupJob(service).execute(context))
                .isInstanceOfSatisfying(JobExecutionException.class,
                        failure -> assertThat(failure.refireImmediately()).isFalse());

        verifyNoInteractions(service);
    }

    @Test
    void cleanupJobRecordsServiceFailureWithoutImmediateRefire() {
        ReservationMaintenanceService service = mock(ReservationMaintenanceService.class);
        JobExecutionContext context = context("expired-reservation-cleanup", "frequent-sweep",
                new JobDataMap(java.util.Map.of("batchSize", "50")));
        org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
                .when(service).completeExpiredReservations(50);

        assertThatThrownBy(() -> new ExpiredReservationCleanupJob(service).execute(context))
                .isInstanceOfSatisfying(JobExecutionException.class,
                        failure -> assertThat(failure.refireImmediately()).isFalse());

        verify(service).completeExpiredReservations(50);
    }

    @Test
    void digestJobDelegatesOnceAndEmitsAggregate() throws Exception {
        ReservationMaintenanceService service = mock(ReservationMaintenanceService.class);
        JobExecutionContext context = context("daily-resource-utilization-digest", "daily-digest",
                new JobDataMap(java.util.Map.of("reportWindowDays", "1")));
        ResourceUtilizationDigest digest = new ResourceUtilizationDigest(
                LocalDate.parse("2026-05-14"),
                LocalDate.parse("2026-05-15"),
                Instant.parse("2026-05-13T21:00:00Z"),
                Instant.parse("2026-05-14T21:00:00Z"),
                List.of(new ResourceUtilizationRow(UUID.fromString("11111111-1111-1111-1111-111111111111"), 1, 60))
        );
        when(service.generateUtilizationDigest(1)).thenReturn(digest);

        new DailyResourceUtilizationDigestJob(service).execute(context);

        verify(service).generateUtilizationDigest(1);
    }

    @Test
    void digestJobRejectsMissingParametersWithoutCallingService() {
        ReservationMaintenanceService service = mock(ReservationMaintenanceService.class);
        JobExecutionContext context = context("daily-resource-utilization-digest", "daily-digest", new JobDataMap());

        assertThatThrownBy(() -> new DailyResourceUtilizationDigestJob(service).execute(context))
                .isInstanceOfSatisfying(JobExecutionException.class,
                        failure -> assertThat(failure.refireImmediately()).isFalse());

        verifyNoInteractions(service);
    }

    private JobExecutionContext context(String jobName, String triggerName, JobDataMap mergedDataMap) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        Trigger trigger = mock(Trigger.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getMergedJobDataMap()).thenReturn(mergedDataMap);
        when(jobDetail.getKey()).thenReturn(new JobKey(jobName, "test-group"));
        when(trigger.getKey()).thenReturn(new TriggerKey(triggerName, "test-group"));
        return context;
    }
}
