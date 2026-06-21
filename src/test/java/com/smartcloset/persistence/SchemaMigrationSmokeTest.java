package com.smartcloset.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema-migration-smoke;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "smartcloset.seed.enabled=false"
})
class SchemaMigrationSmokeTest {

    private static final List<String> ENTITY_TABLES = List.of(
            "users",
            "refresh_sessions",
            "account_action_tokens",
            "social_accounts",
            "clothing_items",
            "recommendation_results",
            "recommendation_result_items",
            "wear_histories"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayBaselineCreatesCurrentEntitySchemaBeforeHibernateValidate() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = true",
                Integer.class
        );
        List<String> entityTables = jdbcTemplate.queryForList(
                """
                        select lower(table_name)
                        from information_schema.tables
                        where table_schema = 'PUBLIC'
                          and lower(table_name) in (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                String.class,
                ENTITY_TABLES.toArray()
        );

        assertThat(appliedMigrations).isEqualTo(1);
        assertThat(entityTables).containsExactlyInAnyOrderElementsOf(ENTITY_TABLES);
    }
}
