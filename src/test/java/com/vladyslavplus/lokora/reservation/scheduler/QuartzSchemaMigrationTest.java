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
                        "001-create-workspace-resource",
                        "002-create-reservation",
                        "003-complete-reservation-status",
                        "004-create-quartz-schema"
                );
    }

    @Test
    void postgresQuartzSchemaCreatesRequiredTablesWithoutDestructiveStatements() throws IOException {
        String schema = new ClassPathResource("db/changelog/changes/004-create-quartz-schema.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toUpperCase(java.util.Locale.ROOT);

        assertThat(schema).contains(
                "CREATE TABLE QRTZ_JOB_DETAILS",
                "CREATE TABLE QRTZ_TRIGGERS",
                "CREATE TABLE QRTZ_SIMPLE_TRIGGERS",
                "CREATE TABLE QRTZ_CRON_TRIGGERS",
                "CREATE TABLE QRTZ_SIMPROP_TRIGGERS",
                "CREATE TABLE QRTZ_BLOB_TRIGGERS",
                "CREATE TABLE QRTZ_CALENDARS",
                "CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS",
                "CREATE TABLE QRTZ_FIRED_TRIGGERS",
                "CREATE TABLE QRTZ_SCHEDULER_STATE",
                "CREATE TABLE QRTZ_LOCKS"
        );
        assertThat(schema).doesNotContain("DROP TABLE", "COMMIT;");
    }
}
