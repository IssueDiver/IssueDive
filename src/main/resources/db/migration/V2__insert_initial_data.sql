-- V2__insert_initial_data.sql
    -- 테스트 목적

-- 테스트용 사용자 추가 (비밀번호는 'password'를 암호화한 값 예시)
INSERT INTO users (id, username, email, password) VALUES
    (1, 'testuser', 'test@example.com', '$2a$10$N.ps3jC1V2doSB.qG0aL/e.UP1s1b4yVw2yWvnsd2z5K/kY2.Y0Cu');

-- (선택) 테스트용 라벨 추가
INSERT INTO label (id, name, color, description) VALUES
                                                     (1, 'bug', '#d73a4a', 'Something isn''t working'),
                                                     (2, 'feature', '#007bff', 'New feature or request');