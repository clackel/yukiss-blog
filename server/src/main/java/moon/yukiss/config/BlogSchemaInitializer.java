package moon.yukiss.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.schema-initializer.enabled", havingValue = "true", matchIfMissing = true)
public class BlogSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public BlogSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createBaseTables();
        addColumnIfMissing("article_comment", "parent_id", "INT NULL");
        addIndexIfMissing(
                "article_comment",
                "idx_article_comment_article_time",
                "(article_id, create_time)"
        );
        upgradeArticleContentColumn();
    }

    private void createBaseTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(20) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    nickname VARCHAR(30) NULL,
                    email VARCHAR(120) NULL,
                    email_verified TINYINT(1) NOT NULL DEFAULT 0,
                    avatar VARCHAR(255) NULL,
                    bio VARCHAR(300) NULL,
                    gender VARCHAR(20) NULL,
                    birthday DATE NULL,
                    location VARCHAR(80) NULL,
                    website VARCHAR(180) NULL,
                    role VARCHAR(20) NOT NULL DEFAULT 'USER',
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_login_time DATETIME NULL,
                    password_updated_time DATETIME NULL,
                    deleted_time DATETIME NULL,
                    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
                    UNIQUE KEY uk_user_username (username),
                    UNIQUE KEY uk_user_email (email)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS article (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    author_id INT NOT NULL,
                    title VARCHAR(80) NOT NULL,
                    content MEDIUMTEXT NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_article_author (author_id),
                    KEY idx_article_created (create_time)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS article_like (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    article_id INT NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_article_like_user_article (user_id, article_id),
                    KEY idx_article_like_article (article_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS article_comment (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    content VARCHAR(500) NOT NULL,
                    article_id INT NOT NULL,
                    user_id INT NOT NULL,
                    parent_id INT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_article_comment_article (article_id),
                    KEY idx_article_comment_parent (parent_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS comment_like (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    comment_id INT NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_comment_like_user_comment (user_id, comment_id),
                    KEY idx_comment_like_comment (comment_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_follow (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    follower_id INT NOT NULL,
                    following_id INT NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_follow_relation (follower_id, following_id),
                    KEY idx_user_follow_follower (follower_id),
                    KEY idx_user_follow_following (following_id)
                )
                """);
    }

    private void upgradeArticleContentColumn() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article'",
                Integer.class
        );
        if (tableCount == null || tableCount == 0) {
            return;
        }

        String dataType = jdbcTemplate.query(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'content'",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null
        );
        if (dataType != null && !"mediumtext".equalsIgnoreCase(dataType) && !"longtext".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("UPDATE article SET content = '' WHERE content IS NULL");
            jdbcTemplate.execute("ALTER TABLE article MODIFY COLUMN content MEDIUMTEXT NOT NULL");
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        if (tableCount == null || tableCount == 0) {
            return;
        }

        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String columns) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        if (tableCount == null || tableCount == 0) {
            return;
        }

        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (indexCount != null && indexCount == 0) {
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + " " + columns);
        }
    }
}
