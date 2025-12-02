package com.halenteck.demo.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Automatically migrates database constraints on application startup.
 * This ensures all developers get the latest constraint definitions without manual SQL execution.
 */
@Service
@Order(1) // Run early in startup
public class DatabaseMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationService.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    @Transactional
    public void migrateDatabaseConstraints() {
        try {
            migrateAuditLogActionConstraint();
        } catch (Exception e) {
            logger.error("Failed to migrate database constraints", e);
            // Don't throw - allow application to start even if migration fails
            // The constraint might already be correct or the table might not exist yet
        }
    }

    private void migrateAuditLogActionConstraint() {
        try {
            // Check if the table exists
            List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'study_audit_logs'",
                String.class
            );

            if (tables.isEmpty()) {
                logger.info("study_audit_logs table does not exist yet, skipping constraint migration");
                return;
            }

            // Check if constraint exists
            List<String> constraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                "WHERE table_schema = 'public' AND table_name = 'study_audit_logs' " +
                "AND constraint_name = 'study_audit_logs_action_check'",
                String.class
            );

            // Allowed values from StudyAuditAction enum
            String allowedValues = "'STUDY_CREATED','STUDY_UPDATED','STUDY_PUBLISHED','STUDY_CLOSED'," +
                    "'STUDY_ARCHIVED','COLLABORATOR_ADDED','COLLABORATOR_ROLE_CHANGED','COLLABORATOR_REMOVED'," +
                    "'QUIZ_ASSIGNED','TASK_ASSIGNED','TASK_COMPLETED','ARTIFACT_LINKED','ARTIFACT_UNLINKED'," +
                    "'RATING_CRITERION_ADDED','RATING_CRITERION_UPDATED','RATING_CRITERION_REMOVED'," +
                    "'INVITE_CREATED','INVITE_ACCEPTED','AUDIT_LOG_EXPORTED'";

            if (!constraints.isEmpty()) {
                // Constraint exists, drop it first
                logger.info("Updating study_audit_logs_action_check constraint...");
                jdbcTemplate.execute("ALTER TABLE study_audit_logs DROP CONSTRAINT IF EXISTS study_audit_logs_action_check");
            }

            // Create/update the constraint with all allowed values
            String sql = String.format(
                "ALTER TABLE study_audit_logs ADD CONSTRAINT study_audit_logs_action_check CHECK (action IN (%s))",
                allowedValues
            );
            jdbcTemplate.execute(sql);
            logger.info("Successfully updated study_audit_logs_action_check constraint");

        } catch (Exception e) {
            logger.warn("Could not migrate study_audit_logs_action_check constraint: " + e.getMessage());
            // This might fail if the constraint already exists with the correct values
            // or if there's a permission issue, but we don't want to block startup
        }
    }
}

