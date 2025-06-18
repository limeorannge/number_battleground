# 1) 빌드 단계: Maven + JDK 17
FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2) 런타임 단계: JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar ./app.jar
ENTRYPOINT ["java","-jar","app.jar"]
