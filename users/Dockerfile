FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY . .

RUN chmod +x ./gradlew && ./gradlew :users:bootJar -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/users/build/libs/*.jar app.jar

EXPOSE 8082
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "app.jar"]

#test
#123456790
#485

