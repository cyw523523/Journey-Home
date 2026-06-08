package com.guitu.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaCompatibilityRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        migrateAdoptionAgreementContentColumn();
        ensureAdoptionAgreementSignatureColumns();
    }

    private void migrateAdoptionAgreementContentColumn() {
        String dataType = jdbcTemplate.query(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'adoption_agreements' AND COLUMN_NAME = 'content'",
                rs -> rs.next() ? rs.getString(1) : null
        );

        if (dataType == null) {
            return;
        }

        if (!"longtext".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("ALTER TABLE adoption_agreements MODIFY COLUMN content LONGTEXT NOT NULL");
        }
    }

    private void ensureAdoptionAgreementSignatureColumns() {
        ensureColumnExists(
                "adoption_agreements",
                "adopter_signature_image_url",
                "ALTER TABLE adoption_agreements ADD COLUMN adopter_signature_image_url VARCHAR(500) NULL AFTER adopter_signature_name"
        );
        ensureColumnExists(
                "adoption_agreements",
                "counterpart_signature_image_url",
                "ALTER TABLE adoption_agreements ADD COLUMN counterpart_signature_image_url VARCHAR(500) NULL AFTER counterpart_signature_name"
        );
    }

    private void ensureColumnExists(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
