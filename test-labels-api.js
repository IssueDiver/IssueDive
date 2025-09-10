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
        'http_req_duration': ['p(95)<1000'], // 95%의 요청이 1초 안에 응답해야 함
        'http_req_failed': ['rate<0.01'],   // 요청 실패율은 1% 미만
    },
};

// 2. 가상 사용자가 실행할 테스트 함수
export default function () {
    const res = http.get('http://localhost:8080/labels');
    check(res, { 'status was 200': (r) => r.status === 200 });
    sleep(1);
}
