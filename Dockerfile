# ==========================================
# Multi-stage Dockerfile
# Angular 19 + Spring Boot 4 + Java 21
# ==========================================

# ==========================================
# Stage 1: Build Angular Frontend
# ==========================================
FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend

# Copy package.json only (ignore stale package-lock.json from previous Angular version)
COPY frontend/package.json ./

# Install dependencies — force resolves peer dependency conflicts during Angular major upgrades
RUN npm install --force && npm cache clean --force

# Copy frontend source
COPY frontend/ ./

# Build Angular application for production
RUN npx ng build --configuration production

# ==========================================
# Stage 2: Build Spring Boot Backend
# ==========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /app/backend

# Copy pom.xml first (for better layer caching)
COPY backend/pom.xml ./

# Download dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy backend source
COPY backend/src ./src

# Build the application (skip tests and test compilation for Docker build)
RUN mvn clean package -Dmaven.test.skip=true -B

# ==========================================
# Stage 3: Runtime Image
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install nginx
RUN apk add --no-cache nginx && mkdir -p /run/nginx

# Copy built Angular app
COPY --from=frontend-builder /app/frontend/dist/immocare-frontend /usr/share/nginx/html

# Copy built Spring Boot jar
COPY --from=backend-builder /app/backend/target/*.jar app.jar

# Copy nginx configuration
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/default.conf /etc/nginx/http.d/default.conf

# Copy startup script
COPY docker/startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

# Expose ports
EXPOSE 80 8080

# Health check — Spring Boot actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/app/startup.sh"]
