## IssueDive

## 프로젝트 개요
IssueDive는 협업 툴의 핵심 기능(이슈 관리, 라벨 관리, 댓글 관리, 사용자 인증)을 구현한 백엔드 프로젝트입니다.
JWT 기반 인증, AWS 배포, 로깅 및 모니터링까지 포함하여 실제 서비스 운영과 유사한 환경을 경험할 수 있도록 설계되었습니다.

## 주요 기능
- **Auth & User**
  - 회원가입, 로그인, JWT 발급/재발급, 사용자 조회, 로그아웃
- **Issue**
  - 이슈 생성, 필터링/검색, 상세 조회, 수정, 상태 변경, 삭제
- **Label**
  - 라벨 생성/조회/수정/삭제, 이슈-라벨 매핑
- **Comment**
  - 댓글 작성/조회/수정/삭제, 대댓글 트리 구조
- **공통**
  - 공통 응답 포맷(`ApiCommonResponse`)
  - 전역 예외 처리(`GlobalExceptionHandler`)
  - Swagger API 문서
 
  ## 기술 스택
  **Backend**
- Java 17  
- Spring Boot 3.5  
- Spring Security, JWT  
- JPA/Hibernate, QueryDSL  
- JUnit5  

**Database**
- MySQL 8.0.x  
- H2 (테스트)  
- Redis (JWT Blacklist, 캐싱)  

**Frontend**
- Vue.js  

**DevOps & Infra**
- Docker, AWS (EC2, RDS, S3)  
- GitHub Actions (CI/CD)  
- Flyway (DB Migration)  
- K6 (성능 테스트)  
- owasp-java-html-sanitizer  

**Monitoring**
- Spring Boot Actuator  
- Prometheus  
- Grafana  

**Tools**
- IntelliJ, VSCode  
- MySQL Workbench, Postman, Swagger  
- Notion, Mermaid, DBdiagram  
- ChatGPT, Gemini, Claude

## 프로젝트 구조
src
 ├─ main/java/com/issueDive
 │   ├─ IssueDiveApplication.java
 │   ├─ config/         # QueryDSL, Security, Swagger 설정
 │   ├─ controller/     # Auth, Issue, Label, Comment 컨트롤러
 │   ├─ dto/            # 요청/응답 DTO
 │   ├─ entity/         # JPA 엔티티 (User, Issue, Label, Comment 등)
 │   ├─ exception/      # 예외 정의 및 전역 핸들러
 │   ├─ repository/     # JPA 레포지토리
 │   ├─ security/       # JWT 유틸, 인증 필터, UserDetailsService
 │   ├─ service/        # 비즈니스 로직 서비스
 │   └─ util/           # JwtUtil 등 유틸리티
 │
 ├─ main/resources
 │   ├─ application-prod.yml
 │   └─ db/migration/   # Flyway 마이그레이션 스크립트
 │
 └─ test/java/com/issueDive
     ├─ controller/     # 각 API 단위 테스트
     ├─ service/        # 서비스 계층 단위 테스트
     ├─ security/       # JWT/시큐리티 테스트
     └─ exception/      # 예외 처리 테스트

## 설치 및 실행
### 로컬 실행
```bash
# 프로젝트 클론
git clone https://github.com/IssueDiver/IssueDive.git
cd IssueDive

# 빌드 및 실행
./gradlew build
./gradlew bootRun
```

### Docker 실행
```bash
docker-compose up --build
```

## 환경 변수
| DB_HOST     | RDS 엔드포인트   | issuedive-mysql.cro4kgswk31z.ap-northeast-2.rds.amazonaws.com |
| DB_PORT     | DB 포트         | 3306 |
| DB_NAME     | DB 이름         | issue_dive |
| DB_USER     | DB 사용자       | issue |
| DB_PASSWORD | DB 비밀번호     | **** |

## API 문서
- 로컬 실행 시: [Swagger UI] http://localhost:8080/swagger-ui/index.html
- 배포 서버 실행 시:http://ec2-13-124-124-121.ap-northeast-2.compute.amazonaws.com:8080/swagger-ui/index.html
