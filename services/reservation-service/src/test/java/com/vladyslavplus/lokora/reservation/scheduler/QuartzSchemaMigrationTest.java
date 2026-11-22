package com.vladyslavplus.lokora.reservation.scheduler;

import org.junit.jupiter.api.Test;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzSchemaMigrationTest {

    @Test
    void masterChangelogParsesStatusAndQuartzMigrations() throws Exception {
        ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
        var changeLog = ChangeLogParserFactory.getInstance()
                .getParser("db/changelog/db.changelog-master.yaml", resourceAccessor)
                .parse("db/changelog/db.changelog-master.yaml", new ChangeLogParameters(), resourceAccessor);

        assertThat(changeLog.getChangeSets())
                .extracting(changeSet -> changeSet.getId())
                .containsExactly(
                        "reservation-service-schema",
                        "reservation-service-create-table",
                        "reservation-service-copy-legacy-data",
                        "reservation-service-create-quartz-schema",
                        "reservation-service-copy-legacy-quartz-data"
                );
    }

    @Test
    void postgresQuartzSchemaCreatesRequiredTablesWithoutDestructiveStatements() throws IOException {
        String schema = new ClassPathResource("db/changelog/changes/004-create-quartz-schema.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toUpperCase(java.util.Locale.ROOT);

        assertThat(schema).contains(
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_JOB_DETAILS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_TRIGGERS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_SIMPLE_TRIGGERS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_CRON_TRIGGERS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_SIMPROP_TRIGGERS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_BLOB_TRIGGERS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_CALENDARS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_PAUSED_TRIGGER_GRPS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_FIRED_TRIGGERS",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_SCHEDULER_STATE",
                "CREATE TABLE IF NOT EXISTS RESERVATION_SERVICE.QRTZ_LOCKS"
        );
        assertThat(schema).doesNotContain("DROP TABLE", "COMMIT;");
    }
}
