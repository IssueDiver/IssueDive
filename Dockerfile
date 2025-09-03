# 1. 빌드 및 테스트 스테이지
FROM gradle:8.8.0-jdk17 AS builder

WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 관련 파일 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 의존성 다운로드 (네트워크를 사용하는 단계이므로 먼저 실행)
RUN ./gradlew --no-daemon dependencies

# 전체 소스 복사
COPY src ./src

# assemble 대신 build 명령어를 사용해 테스트까지 함께 실행합니다.
# Spring Boot의 테스트 프로필을 활성화하여 H2 DB를 사용하도록 합니다.
RUN ./gradlew build --no-daemon -Dspring.profiles.active=test


# 2. 실행 스테이지
FROM openjdk:17-jdk-slim

WORKDIR /app

# 빌드 산출물(테스트가 통과된)을 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 실행
ENTRYPOINT ["java", "-jar", "app.jar"]