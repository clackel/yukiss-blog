package moon.yukiss.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.schema-initializer.enabled", havingValue = "true", matchIfMissing = true)
public class UserSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public UserSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("email", "VARCHAR(120) NULL");
        addColumnIfMissing("email_verified", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("bio", "VARCHAR(300) NULL");
        addColumnIfMissing("gender", "VARCHAR(20) NULL");
        addColumnIfMissing("birthday", "DATE NULL");
        addColumnIfMissing("location", "VARCHAR(80) NULL");
        addColumnIfMissing("website", "VARCHAR(180) NULL");
        addColumnIfMissing("last_login_time", "DATETIME NULL");
        addColumnIfMissing("password_updated_time", "DATETIME NULL");
        addColumnIfMissing("deleted_time", "DATETIME NULL");
        addColumnIfMissing("is_deleted", "TINYINT(1) NOT NULL DEFAULT 0");
    }

    private void addColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE user ADD COLUMN " + columnName + " " + definition);
        }
    }
}
