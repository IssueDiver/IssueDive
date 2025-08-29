# 1. 빌드 스테이지
FROM gradle:8.14.3-jdk17 AS builder

WORKDIR /app

# build-arg를 받기 위한 ARG 선언을 추가합니다.
ARG DB_URL
ARG DB_USER
ARG DB_PASSWORD

# gradle.properties 파일을 동적으로 생성합니다.
#RUN echo "dbUrl=${DB_URL}" >> gradle.properties
#RUN echo "dbUser=${DB_USER}" >> gradle.properties
#RUN echo "dbPassword=${DB_PASSWORD}" >> gradle.properties
RUN echo "dbUrl=${DB_URL}" > gradle.properties && \
    echo "dbUser=${DB_USER}" >> gradle.properties && \
    echo "dbPassword=${DB_PASSWORD}" >> gradle.properties

# Gradle 캐시 최적화를 위해 build.gradle과 settings.gradle 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 의존성 다운로드
RUN ./gradlew --no-daemon dependencies

# 나머지 소스 복사 및 빌드
COPY src ./src
RUN ./gradlew assemble --no-daemon -Dspring.profiles.active=test


# 2. 실행 스테이지
FROM openjdk:17-jdk-slim

WORKDIR /app

# 빌드 산출물을 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 실행 시
ENTRYPOINT ["java", "-jar", "app.jar"]

