# ===== Build stage =====
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# 레포 전체 복사
COPY . .

# stores bootJar 생성 (테스트 제외)
RUN chmod +x ./gradlew && ./gradlew :stores:bootJar -x test

# ===== Run stage =====
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 반드시 존재하는 경로에서 jar 복사
COPY --from=builder /app/stores/build/libs/*.jar app.jar

EXPOSE 8083
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "app.jar"]
