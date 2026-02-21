#!/bin/sh
echo "=== ImmoCare Startup ==="
echo "Starting services..."

# Create nginx pid directory (required on Alpine)
mkdir -p /run/nginx

# Start nginx in background
echo "Starting nginx..."
nginx

# Wait for nginx to write its pid file
sleep 2

# Verify nginx is running via pid file
if [ ! -f /run/nginx/nginx.pid ]; then
    echo "ERROR: nginx failed to start"
    cat /var/log/nginx/error.log 2>/dev/null || echo "No error log available"
    exit 1
fi
echo "✓ nginx started successfully"

# Start Spring Boot backend (takes over the process with exec)
echo "Starting Spring Boot backend..."
echo "Database: ${DB_HOST:-postgres}:${DB_PORT:-5432}/${DB_NAME:-immocare}"

exec java \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=8080 \
    -Dspring.datasource.url=jdbc:postgresql://${DB_HOST:-postgres}:${DB_PORT:-5432}/${DB_NAME:-immocare} \
    -Dspring.datasource.username=${DB_USER:-immocare} \
    -Dspring.datasource.password=${DB_PASSWORD:-immocare} \
    -Dspring.flyway.enabled=true \
    -Dspring.jpa.hibernate.ddl-auto=none \
    -jar /app/app.jar
