#!/bin/sh
set -e

echo "=== ImmoCare DEV Startup ==="
echo "Profil actif : ${SPRING_PROFILES_ACTIVE:-postgres}}"

mkdir -p /run/nginx /app/backend/data

# ── 1. Nginx ─────────────────────────────────────────────────────────────
echo "Démarrage nginx (dev)..."
nginx
sleep 1
echo "✓ nginx démarré sur :8080"

# ── 2. Spring Boot ───────────────────────────────────────────────────────
ACTIVE_PROFILE="${SPRING_PROFILES_ACTIVE:-postgres}"
echo "Démarrage Spring Boot → profil ${ACTIVE_PROFILE} sur :8081..."

cd /app/backend


    JVM_ARGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
              -Dserver.port=8081 \
              -Dspring.profiles.active=${ACTIVE_PROFILE}"


MAVEN_OPTS="${MAVEN_OPTS:--Xmx512m}" \
mvn spring-boot:run \
    -DskipTests \
    "-Dspring-boot.run.jvmArguments=${JVM_ARGS}" &

BACKEND_PID=$!
echo "✓ Spring Boot démarré (PID $BACKEND_PID)"

# ── 3. Angular ng serve ───────────────────────────────────────────────────
echo "Démarrage Angular ng serve sur :4200..."
cd /app/frontend
npm start -- --host 0.0.0.0 --port 4200 --poll 2000 --disable-host-check &
FRONTEND_PID=$!
echo "✓ Angular dev server démarré (PID $FRONTEND_PID)"

echo ""
echo "=== Services prêts ==="
echo "  App (nginx) : http://localhost:8080"
echo "  Angular     : http://localhost:4200 (direct)"
echo "  Spring Boot : http://localhost:8081 (direct)"
echo "  Debug JDWP  : localhost:5005"
echo "  POSTGRES TCP: localhost:5432 (SQL client)"
echo ""

wait $BACKEND_PID $FRONTEND_PID