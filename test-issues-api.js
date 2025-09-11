import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 테스트 옵션 설정
export const options = {
    stages: [
        { duration: '1m', target: 1000 },   // 1분간 사용자 1000명까지 증가
        { duration: '2m', target: 1000 },   // 2분간 사용자 1000명 유지
        { duration: '1m', target: 0 },      // 1분간 사용자 0명으로 감소
    ],
    thresholds: {
        'http_req_duration': ['p(95)<1000'],
        'http_req_failed': ['rate<0.01'],
    },
};

// 2. 가상 사용자가 실행할 테스트 함수
export default function () {
    // OPEN 상태인 이슈의 첫 페이지만 반복적으로 조회
    const res = http.get('http://localhost:8080/issues?status=OPEN&page=0&size=20');
    check(res, { 'status was 200': (r) => r.status === 200 });
    sleep(1);
}
