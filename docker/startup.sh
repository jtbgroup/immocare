#!/bin/sh
echo "=== ImmoCare Startup ==="
echo "Profil actif : ${SPRING_PROFILES_ACTIVE:-h2}"

# Créer le répertoire pid nginx (requis sur Alpine)
mkdir -p /run/nginx

# Créer le répertoire data H2 si besoin
mkdir -p /app/data

# Démarrer nginx en arrière-plan
echo "Démarrage nginx..."
nginx

sleep 2

if [ ! -f /run/nginx/nginx.pid ]; then
    echo "ERREUR : nginx n'a pas démarré"
    cat /var/log/nginx/error.log 2>/dev/null || echo "Pas de log disponible"
    exit 1
fi
echo "✓ nginx démarré"

# ── Construire les arguments JVM selon le profil ──────────────────────────

ACTIVE_PROFILE="${SPRING_PROFILES_ACTIVE:-h2}"

if [ "$ACTIVE_PROFILE" = "postgres" ] || [ "$ACTIVE_PROFILE" = "production" ]; then
    echo "Démarrage Spring Boot → PostgreSQL"
    echo "Base : ${DB_HOST:-postgres}:${DB_PORT:-5432}/${DB_NAME:-immocare}"
    DB_ARGS="-Dspring.datasource.url=jdbc:postgresql://${DB_HOST:-postgres}:${DB_PORT:-5432}/${DB_NAME:-immocare} \
             -Dspring.datasource.username=${DB_USER:-immocare} \
             -Dspring.datasource.password=${DB_PASSWORD:-immocare} \
             -Dspring.flyway.enabled=true \
             -Dspring.jpa.hibernate.ddl-auto=none"
else
    echo "Démarrage Spring Boot → H2 (fichier /app/data/immocare)"
    DB_ARGS="-Dspring.datasource.url='jdbc:h2:file:/app/data/immocare;AUTO_SERVER=TRUE;NON_KEYWORDS=VALUE' \
             -Dspring.datasource.driverClassName=org.h2.Driver \
             -Dspring.datasource.username=sa \
             -Dspring.datasource.password= \
             -Dspring.flyway.enabled=true \
             -Dspring.jpa.hibernate.ddl-auto=validate \
             -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
fi

# Démarrer Spring Boot (prend la main avec exec)
exec java \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=8080 \
    -Dspring.profiles.active="${ACTIVE_PROFILE}" \
    ${JAVA_OPTS} \
    ${DB_ARGS} \
    -jar /app/app.jar