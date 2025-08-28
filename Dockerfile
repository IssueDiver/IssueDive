# 1. 빌드 스테이지
FROM gradle:8.14.3-jdk17 AS builder

WORKDIR /app

# Gradle 캐시 최적화를 위해 build.gradle과 settings.gradle 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 의존성 다운로드
RUN ./gradlew --no-daemon dependencies

# 나머지 소스 복사 및 빌드
COPY src ./src
# RUN ./gradlew build --no-daemon -Dspring.profiles.active=test
RUN ./gradlew assemble --no-daemon -Dspring.profiles.active=test


# 2. 실행 스테이지
FROM openjdk:17-jdk-slim

WORKDIR /app

# 빌드 산출물을 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 실행 시
ENTRYPOINT ["java", "-jar", "app.jar"]
