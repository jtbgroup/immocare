#!/bin/bash
# Reset database and rebuild from clean migrations
# WARNING: destroys all data

echo "=== ImmoCare - Clean Reset ==="
echo "⚠️  This will destroy all data in the database!"
read -p "Are you sure? [y/N] " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

# Stop everything
docker compose down

# Drop and recreate the database volume
docker volume rm immocare_postgres_data 2>/dev/null || true

# Rebuild and start
docker compose up -d --build

echo ""
echo "✓ Clean reset complete"
echo "  Credentials: admin / admin123"
