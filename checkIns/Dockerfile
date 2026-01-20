# QR 체크인 서비스 Dockerfile
FROM openjdk:17-slim

# 작업 디렉토리 설정
WORKDIR /app

# 애플리케이션 JAR 파일 복사
COPY build/libs/checkIns-0.0.1-SNAPSHOT.jar app.jar

# 포트 노출
EXPOSE 8082

# 시간대 설정
ENV TZ=Asia/Seoul

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:local}", "app.jar"]