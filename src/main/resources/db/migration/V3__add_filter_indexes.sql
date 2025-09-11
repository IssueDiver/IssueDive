-- V3__Add_Filter_Indexes.sql: 쿼리 성능 향상을 위한 필터링 인덱스 추가

-- 이슈 테이블 인덱스
-- 이슈 목록 조회 시 status와 author_id는 필터링 조건으로 매우 자주 사용되므로 인덱싱합니다.
CREATE INDEX idx_issue_status ON issue (_status);
CREATE INDEX idx_issue_author_id ON issue (author_id);