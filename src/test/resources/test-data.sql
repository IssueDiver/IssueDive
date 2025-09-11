-- src/test/resources/test-data.sql
DELETE FROM users WHERE id = 1;

INSERT INTO users (id, username, email, password, created_at, updated_at)
VALUES (1, 'testuser', 'test@example.com', 'password', NOW(), NOW());