# syntax=docker/dockerfile:1.6

# --- Stage 1: build the React frontend ---
FROM node:20-alpine AS frontend-build
WORKDIR /fe
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# --- Stage 2: run backend tests (fails the build on any test failure) ---
FROM maven:3.9-eclipse-temurin-21 AS backend-test
WORKDIR /workspace
COPY backend/pom.xml .
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN echo "=================== RUNNING BACKEND TESTS ===================" \
 && mvn -B test -Dsurefire.useFile=false \
 && echo "=================== BACKEND TESTS PASSED ==================="

# --- Stage 3: build the Spring Boot JAR (tests already ran in stage 2) ---
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace
COPY --from=backend-test /root/.m2 /root/.m2
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -B -DskipTests package

# --- Stage 4: runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend-build /workspace/target/importer.jar /app/importer.jar
COPY --from=frontend-build /fe/dist /app/frontend/
ENV FRONTEND_DIR=file:/app/frontend/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/importer.jar"]
