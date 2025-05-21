# ---------- Stage 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Копируем файлы проекта
COPY pom.xml .
COPY src ./src

RUN mvn clean install -DskipTests

# ---------- Stage 2: Runtime ----------
FROM amazoncorretto:17

WORKDIR /app

# Копируем JAR-файл из предыдущего stage
COPY --from=build /app/target/*.jar application.jar

EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-Xmx2048M", "-jar", "/app/application.jar"]