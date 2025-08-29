CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE label (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(50) NOT NULL UNIQUE,
                        color VARCHAR(7) NOT NULL,
                        description TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE issue (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        _status ENUM('OPEN', 'CLOSED') DEFAULT 'OPEN',
                        author_id BIGINT NOT NULL,
                        assignee_id BIGINT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (author_id) REFERENCES users(id),
                        FOREIGN KEY (assignee_id) REFERENCES users(id)
);

CREATE TABLE issue_label (
                              issue_id BIGINT,
                              label_id BIGINT,
                              PRIMARY KEY (issue_id, label_id),
                              FOREIGN KEY (issue_id) REFERENCES issue(id),
                              FOREIGN KEY (label_id) REFERENCES label(id)
);

CREATE TABLE comment (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          issue_id BIGINT NOT NULL,
                          author_id BIGINT NOT NULL,
                          parent_id BIGINT NULL,
                          description TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          FOREIGN KEY (issue_id) REFERENCES issue(id),
                          FOREIGN KEY (author_id) REFERENCES users(id),
                          FOREIGN KEY (parent_id) REFERENCES comment(id)
);
