#!/bin/sh
echo "=== ImmoCare Startup ==="
echo "Profil actif : ${SPRING_PROFILES_ACTIVE:-postgres}}"

# Créer le répertoire pid nginx (requis sur Alpine)
mkdir -p /run/nginx

# Créer le répertoire data si besoin
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

ACTIVE_PROFILE="${SPRING_PROFILES_ACTIVE:-postgres}"


    echo "Démarrage Spring Boot → PostgreSQL"
    echo "Base : ${DB_HOST:-postgres}:${DB_PORT:-5432}/${DB_NAME:-immocare}"
    DB_ARGS="-Dspring.datasource.url=jdbc:postgresql://${DB_HOST:-postgres}:${DB_PORT:-5432}/${DB_NAME:-immocare} \
             -Dspring.datasource.username=${DB_USER:-immocare} \
             -Dspring.datasource.password=${DB_PASSWORD:-immocare} \
             -Dspring.flyway.enabled=true \
             -Dspring.jpa.hibernate.ddl-auto=none"


# Démarrer Spring Boot (prend la main avec exec)
exec java \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=8080 \
    -Dspring.profiles.active="${ACTIVE_PROFILE}" \
    ${JAVA_OPTS} \
    ${DB_ARGS} \
    -jar /app/app.jar