-- V1__init.sql

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL
);

CREATE TABLE label (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE,
                       color VARCHAR(7) NOT NULL,
                       description TEXT,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP
);

CREATE TABLE issue (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       _status ENUM('OPEN', 'IN_PROGRESS', 'CLOSED') NOT NULL DEFAULT 'OPEN',
                       author_id BIGINT NOT NULL,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE issue_label (
                             issue_id BIGINT NOT NULL,
                             label_id BIGINT NOT NULL,
                             added_at TIMESTAMP NOT NULL,
                             PRIMARY KEY (issue_id, label_id),
                             FOREIGN KEY (issue_id) REFERENCES issue(id) ON DELETE CASCADE,
                             FOREIGN KEY (label_id) REFERENCES label(id) ON DELETE CASCADE
);

CREATE TABLE comment (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         issue_id BIGINT NOT NULL,
                         author_id BIGINT NOT NULL,
                         parent_id BIGINT NULL,
                         description TEXT NOT NULL,
                         created_at TIMESTAMP,
                         updated_at TIMESTAMP,
                         FOREIGN KEY (issue_id) REFERENCES issue(id) ON DELETE CASCADE,
                         FOREIGN KEY (author_id) REFERENCES users(id),
                         FOREIGN KEY (parent_id) REFERENCES comment(id) ON DELETE CASCADE
);

CREATE TABLE issue_assignee (
                                issue_id BIGINT NOT NULL,
                                user_id BIGINT NOT NULL,
                                PRIMARY KEY (issue_id, user_id),
                                FOREIGN KEY (issue_id) REFERENCES issue (id) ON DELETE CASCADE,
                                FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);