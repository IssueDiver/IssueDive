-- 기존 issue 테이블의 assignee_id 외래 키 제약 조건 삭제
ALTER TABLE issue DROP FOREIGN KEY `issue_ibfk_2`;

-- 기존 assignee_id 컬럼 삭제
ALTER TABLE issue DROP COLUMN assignee_id;

-- 이슈와 담당자(사용자)의 다대다 관계를 위한 조인 테이블 생성
CREATE TABLE issue_assignee (
                                issue_id BIGINT NOT NULL,
                                user_id BIGINT NOT NULL,
                                PRIMARY KEY (issue_id, user_id),
                                FOREIGN KEY (issue_id) REFERENCES issue (id) ON DELETE CASCADE,
                                FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
