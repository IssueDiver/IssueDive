# 🚀 IssueDive - Backend

## GitHub 이슈 트래커 클론 프로젝트
### 프로젝트 개요
> IssueDive는 개발자들의 필수 협업 도구인 GitHub 이슈 트래커의 핵심 기능을 구현한 Full-Stack 프로젝트입니다. </br>
> 이 레포지토리에서는 IssueDive의 <u>**백엔드 API 서버**</u>를 다룹니다. </br>
> JWT 기반 인증, AWS 배포, 성능 최적화, 보안 강화, 모니터링까지 포함하여 실제 서비스 운영과 유사한 환경을 경험할 수 있도록 설계되었습니다.
>
> 🔗 [Go To Frontend Repository](https://github.com/IssueDiver/IssueDive-Front)

 </br>
 
### 🎥 시연 영상
> [▶️ 유튜브에서 보기](https://youtu.be/o04gjUN-XKA?si=qn_dW2u2diJXHXUe)

![Image](https://github.com/user-attachments/assets/fb44b262-b4ee-4ba1-aca1-5d29bfe18e6d)

 </br>

 
## ✨ 주요 기능/비기능 요구사항
|  |  |
| ----------- | ------------------------ |
| 👤 Auth & User | JWT 기반의 Stateless 인증 (회원가입, 자동 로그인, 로그아웃), 사용자 조회 |
| 🎫 Issue | 이슈 생성, 다중 조건 필터링/검색, 상세 조회, 수정, 상태 변경, 삭제 |
| 🏷️ Label | 라벨 생성/조회/수정/삭제, 이슈-라벨 다대다 매핑 | 
| 💬 Comment | 댓글 작성/조회/수정/삭제, 계층형 대댓글 트리 구조 | 
| ⚡ 성능 최적화 | EXPLAIN 분석 기반의 DB 인덱싱, JOIN을 활용한 N+1 문제 해결, k6 부하 테스트를 통한 성능 검증 (가상 사용자 1,000명 환경에서 p(95) < 40ms 달성) | 
| 🛡️ 보안 | 전역 필터를 이용한 XSS 방어 및 @Valid를 통한 강력한 입력값 유효성 검증 | 
| 📜 공통 | ApiCommonResponse를 통한 일관된 응답 포맷, GlobalExceptionHandler를 이용한 전역 예외 처리, Swagger API 문서 자동화 | 

 </br>

## 🛠️ 기술 스택
|  |  |
| ----------- | ------------------------ |
| **Backend** | `Java 17`, `Spring Boot 3.5`, `Spring Security`, `JPA/Hibernate`, `QueryDSL`, `JUnit5` |
| **Database** | `MySQL 8.0`, `H2 (Test)`, `Redis (JWT Blacklist, 캐싱)`, `AWS RDS` |
| **Frontend** | `Vue.js 3`, `Pinia`, `Vue Router`, `Axios`, `Tailwind CSS` |
| **DevOps & Infra** | `Docker`, `Docker Compose`, `AWS EC2`, `Nginx`, `GitHub Actions (CI/CD)`, `Flyway`, `K6` |
| **Monitoring** | `Spring Boot Actuator`, `Prometheus`, `Grafana` |
| **Tools** | `IntelliJ, VSCode`, `MySQL Workbench`, `Postman, Swagger`, `Notion`, `Mermaid, DBdiagram`, `ChatGPT, Gemini, Claude` |

 </br>
 
## 프로젝트 구조
```plaintext
src
 ├─ main/java/com/issueDive
 │   ├─ IssueDiveApplication.java
 │   ├─ config/         # QueryDSL, Security, Swagger, XSS 필터 등 설정
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
 │   └─ db/migration/   # Flyway 마이그레이션 스크립트
 │
 └─ test/java/com/issueDive
     ├─ controller/     # 각 API 단위 테스트
     ├─ service/        # 서비스 계층 단위 테스트
     ├─ security/       # JWT/시큐리티 테스트
     └─ exception/      # 예외 처리 테스트
```

 </br>
 
## ⚙️ 실행 방법

**사전 요구사항**
- Java 17
- Docker 및 Docker Compose

### 1. Docker로 실행하기 (권장)
가장 간단한 방법입니다. Docker가 설치되어 있다면 아래 명령어 하나로 DB와 서버가 함께 실행됩니다.
프로젝트 루트 경로에 .env 파일을 생성하고 아래 내용을 채워주세요.

```bash
MYSQL_ROOT_PASSWORD={YOUR_PASSWORD}
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/issue_dive?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME={YOUR_USERNAME}
SPRING_DATASOURCE_PASSWORD={YOUR_PASSWORD}
```

아래 명령어를 실행합니다.
```bash
docker-compose up --build -d
```

 </br>
 
### 2. 로컬에서 직접 실행하기
로컬에 MySQL이 설치 및 실행 중이어야 합니다.
src/main/resources/application.properties 파일의 spring.datasource 정보를 로컬 DB 환경에 맞게 수정합니다.
아래 명령어로 애플리케이션을 빌드하고 실행합니다.

```bash
./gradlew bootRun
```

 </br>

### 확인
**API 문서**: http://localhost:8080/swagger-ui/index.html </br>
**Actuator (모니터링)**: http://localhost:8080/actuator 

</br>


## 환경 변수
| 이름        | 설명          | 예시 |
|-------------|---------------|---------------------------------------------|
| DB_HOST     | RDS 엔드포인트 | issuedive-mysql.cro4kgswk31z.ap-northeast-2.rds.amazonaws.com |
| DB_PORT     | DB 포트       | 3306 |
| DB_NAME     | DB 이름       | issue_dive |
| DB_USER     | DB 사용자     | issue |
| DB_PASSWORD | DB 비밀번호   | password |


 </br>
 
## 👨‍💻 팀원 소개
|  |  | 
| ------- | ------------------------ |
| 은지우 [@meraki6512](https://github.com/meraki6512) | 팀장, BE(Issue) 및 FE(전체) 개발, CI/CD 구축, 성능/보안 개선, 문서화 |
| 김소연 [@soyeonkim8888](https://github.com/soyeonkim8888) | BE(Auth), 통합 테스트 |
| 이성채 [@sungchaelee](https://github.com/sungchaelee) | BE(Comment), 모니터링, 성능 개선| 
| 박세현 [@tpgus1221](https://github.com/tpgus1221) | BE(Label), AWS 배포, API 테스트 |


 </br>
 
## 📜 주요 산출물
> 프로젝트의 모든 기획, 설계, 분석 문서는 아래에서 확인하실 수 있습니다.

최종 기획안: [최종 기획안 Docs](https://docs.google.com/document/d/1jfvVWbFgSDaRamZesPRmYq2iongn2wFI7nE7zk9sBHE/edit?usp=sharing)</br>
발표 자료 (PPT): [Canva](https://goorm1.my.canva.site/srs) </br>
API 문서: [API 문서: 구글 Docs](https://docs.google.com/document/d/1diZlPkwtpfGbaEFJLG5SJ9BtdPSveXviEPdpxa_v68o/edit?usp=sharing)</br>
기타 산출물: [아키텍처](https://docs.google.com/document/d/1e0JMy1z6Nv5pb8Lywc3qrn2HuxVPGVb1RBexY8rbz2E/edit?usp=sharing), [ERD](https://docs.google.com/document/d/1h-k7Cc9a9ARHyEtdl79sAzWNCa4oorkFAdWqZq1txNQ/edit?usp=sharing), [UseCase](https://docs.google.com/document/d/1PGSe9AeDiN5K-a2h0W_j1vNkOygp08VjHJvU5uDO54Y/edit?usp=sharing), [성능 개선 리포트](https://docs.google.com/document/d/1kvfYity9uiEIDRoL7CH8wyMazn8fBueM_yYeXI6dyYE/edit?usp=sharing), [보안 개선 리포트](https://docs.google.com/document/d/1xnmfO1ng1z5CZWww3pXyERA_fSbJ8TcRGBN_yEBK0Ow/edit?usp=sharing)

### Diagram
<table>
<tr>
<td align="center"><strong>ERD (데이터베이스 구성도)</strong></td>
<td align="center"><strong>Use Case Diagram</strong></td>
</tr>
<tr>
<td><img width="400" alt="ERD (데이터베이스 구성도)" src="https://github.com/user-attachments/assets/81adbe8e-6408-4158-8aee-6bdce650bf3e" /></td>
<td><img width="400" alt="Use Case Diagram" src="https://github.com/user-attachments/assets/1c6ea4eb-b2cf-40cb-bd00-4e8c1fafab69" /></td>
</tr>
</table>



### Architecture
<table>
<tr>
<td align="center"><strong>백엔드 상세 아키텍처 (Layered)</strong></td>
<td align="center"><strong>CI/CD 및 배포 아키텍처</strong></td>
</tr>
<tr>
<td><img width="400" alt="Backend Architecture" src="https://github.com/user-attachments/assets/49b66fd2-0c60-4ea3-acde-4a0f04f92f6d" /></td>
<td><img width="400" alt="CI/CD Architecture" src="https://github.com/user-attachments/assets/aeaad4c3-6490-46f3-8611-bba1530b9766" /></td>
</tr>
</table>

<br>


