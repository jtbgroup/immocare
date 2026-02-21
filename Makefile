# ImmoCare - Makefile
.PHONY: help build up down restart logs shell db-shell backup restore \
        clean clean-all rebuild dev prod health install update \
        backend-test nginx-reload nginx-test \
        start stop dev-build logs-dev

# Variables
APP_NAME = immocare
VERSION  = 1.0
BACKUP_DIR = ./backups

help: ## Show available commands
	@echo "ImmoCare v$(VERSION) - Available commands:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ── Production ────────────────────────────────────────────────────────────────

build: ## Build Docker image
	@echo "Building $(APP_NAME):$(VERSION)..."
	docker compose build

up: ## Start services
	@echo "Starting services..."
	docker compose up -d
	@echo "✓ Services started"
	@echo "  Application : http://localhost:8090"
	@echo "  Health      : http://localhost:8080/actuator/health"

down: ## Stop services
	@echo "Stopping services..."
	docker compose down
	@echo "✓ Services stopped"

restart: ## Restart services
	docker compose restart
	@echo "✓ Services restarted"

prod: ## Build and start in production mode
	docker compose up -d --build
	@echo "✓ Production deployment complete"
	@echo "  Application : http://localhost:8090"

rebuild: ## Full rebuild without cache
	@echo "Rebuilding from scratch..."
	docker compose build --no-cache
	@echo "✓ Rebuild complete"

# ── Development (daily use) ───────────────────────────────────────────────────

start: ## ▶  Start dev environment WITHOUT rebuild (daily use)
	@echo "Starting dev environment..."
	docker compose -f docker-compose.dev.yml up -d
	@echo "✓ Dev environment started (no rebuild)"
	@echo "  Frontend : http://localhost:4200"
	@echo "  Backend  : http://localhost:8080"

stop: ## ⏹  Stop dev environment
	@echo "Stopping dev environment..."
	docker compose -f docker-compose.dev.yml down
	@echo "✓ Dev environment stopped"

dev-build: ## 🔨 Rebuild dev images (only after pom.xml / package.json changes)
	@echo "Rebuilding dev images..."
	docker compose -f docker-compose.dev.yml up -d --build
	@echo "✓ Dev images rebuilt and started"
	@echo "  Frontend : http://localhost:4200"
	@echo "  Backend  : http://localhost:8080"

logs-dev: ## View dev logs
	docker compose -f docker-compose.dev.yml logs -f

# ── Legacy dev commands (kept for compatibility) ──────────────────────────────

dev: ## Start in development mode with logs visible (rebuilds — use 'start' for daily use)
	docker compose -f docker-compose.dev.yml up --build

dev-d: ## Start in development mode detached (rebuilds — use 'start' for daily use)
	docker compose -f docker-compose.dev.yml up -d --build
	@echo "✓ Dev services started"
	@echo "  Frontend : http://localhost:4200"
	@echo "  Backend  : http://localhost:8080"

dev-down: ## Stop development services
	docker compose -f docker-compose.dev.yml down
	@echo "✓ Dev services stopped"

# ── Logs ──────────────────────────────────────────────────────────────────────

logs: ## View all logs
	docker compose logs -f

logs-app: ## View application logs
	docker compose logs -f app

logs-db: ## View database logs
	docker compose logs -f postgres

# ── Database ──────────────────────────────────────────────────────────────────

db-shell: ## Open PostgreSQL shell
	docker compose exec postgres psql -U immocare -d immocare

backup: ## Backup database
	@mkdir -p $(BACKUP_DIR)
	@echo "Creating backup..."
	@docker compose exec -T postgres pg_dump -U immocare immocare \
		> $(BACKUP_DIR)/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "✓ Backup created in $(BACKUP_DIR)"

restore: ## Restore database (usage: make restore FILE=backup.sql)
	@if [ -z "$(FILE)" ]; then \
		echo "Error: Please specify FILE=backup.sql"; \
		exit 1; \
	fi
	@echo "Restoring from $(FILE)..."
	@docker compose exec -T postgres psql -U immocare immocare < $(FILE)
	@echo "✓ Backup restored"

# ── Tests ─────────────────────────────────────────────────────────────────────

backend-test: ## Run backend tests
	cd backend && mvn test
	@echo "✓ Backend tests complete"

# ── Shell access ──────────────────────────────────────────────────────────────

shell: ## Open shell in app container
	docker compose exec app sh

shell-db: ## Open shell in database container
	docker compose exec postgres sh

# ── Nginx ─────────────────────────────────────────────────────────────────────

nginx-reload: ## Reload nginx configuration
	docker compose exec app nginx -s reload
	@echo "✓ Nginx configuration reloaded"

nginx-test: ## Test nginx configuration
	docker compose exec app nginx -t

# ── Health & Status ───────────────────────────────────────────────────────────

status: ## Show service status
	docker compose ps

health: ## Check service health
	@echo "Checking service health..."
	@docker compose ps
	@echo ""
	@curl -s -o /dev/null -w "App health : HTTP %{http_code}\n" \
		http://localhost:8090/api/v1/buildings || echo "App: not responding"

monitor: ## Show container resource usage
	docker stats immocare-app immocare-db

# ── Cleanup ───────────────────────────────────────────────────────────────────

clean: ## Remove unused Docker resources
	@echo "Cleaning up..."
	docker system prune -f
	@echo "✓ Cleanup complete"

clean-all: ## Remove everything including volumes ⚠️  deletes all data
	@echo "⚠️  WARNING: This will delete all data!"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker compose down -v; \
		docker system prune -a -f; \
		echo "✓ Full cleanup complete"; \
	fi

# ── Installation ──────────────────────────────────────────────────────────────

install: ## Full initial installation
	@echo "=== ImmoCare Installation ==="
	@echo "1. Building images..."
	@$(MAKE) build
	@echo "2. Starting services..."
	@$(MAKE) up
	@echo "3. Waiting for services to be ready..."
	@sleep 15
	@echo "4. Checking health..."
	@$(MAKE) health
	@echo ""
	@echo "✓ Installation complete!"
	@echo "  Application : http://localhost:8090"
	@echo "  Credentials : admin / Admin1234!"

update: ## Update application (pull + rebuild + restart)
	@echo "Updating application..."
	@git pull
	@$(MAKE) down
	@$(MAKE) build
	@$(MAKE) up
	@echo "✓ Update complete"