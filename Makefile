.PHONY: help up down dev logs restart rebuild clean test backend-test frontend-test db-shell health

# Default target
.DEFAULT_GOAL := help

help: ## Show this help message
	@echo "ImmoCare - Docker Commands"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Production commands
up: ## Start all containers (production mode)
	docker-compose up -d
	@echo "✅ All containers started!"
	@echo "🌐 Frontend: http://localhost:4200"
	@echo "🔧 Backend API: http://localhost:8080"
	@echo "💚 Health: http://localhost:8080/actuator/health"

down: ## Stop all containers
	docker-compose down
	@echo "✅ All containers stopped"

clean: ## Stop containers and remove volumes (⚠️  deletes database)
	docker-compose down -v
	@echo "✅ All containers and volumes removed"

# Development commands
dev: ## Start in development mode (hot reload)
	docker-compose -f docker-compose.dev.yml up
	@echo "🔥 Development mode with hot reload"

dev-d: ## Start in development mode (detached)
	docker-compose -f docker-compose.dev.yml up -d
	@echo "✅ Development containers started in background"

# Logs
logs: ## View logs from all containers
	docker-compose logs -f

logs-backend: ## View backend logs
	docker-compose logs -f backend

logs-frontend: ## View frontend logs
	docker-compose logs -f frontend

logs-db: ## View database logs
	docker-compose logs -f postgres

# Restart
restart: ## Restart all containers
	docker-compose restart
	@echo "✅ All containers restarted"

restart-backend: ## Restart backend only
	docker-compose restart backend
	@echo "✅ Backend restarted"

restart-frontend: ## Restart frontend only
	docker-compose restart frontend
	@echo "✅ Frontend restarted"

# Rebuild
rebuild: ## Rebuild and restart all containers
	docker-compose up -d --build
	@echo "✅ All containers rebuilt and started"

rebuild-backend: ## Rebuild backend only
	docker-compose up -d --build backend
	@echo "✅ Backend rebuilt"

rebuild-frontend: ## Rebuild frontend only
	docker-compose up -d --build frontend
	@echo "✅ Frontend rebuilt"

# Testing
test: backend-test ## Run all tests

backend-test: ## Run backend tests
	cd backend && mvn test
	@echo "✅ Backend tests completed"

frontend-test: ## Run frontend tests
	cd frontend && npm test
	@echo "✅ Frontend tests completed"

# Database
db-shell: ## Open PostgreSQL shell
	docker exec -it immocare-postgres psql -U immocare -d immocare

db-dump: ## Dump database to file
	docker exec immocare-postgres pg_dump -U immocare immocare > backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "✅ Database dumped"

db-restore: ## Restore database from latest dump (usage: make db-restore FILE=backup.sql)
	docker exec -i immocare-postgres psql -U immocare immocare < $(FILE)
	@echo "✅ Database restored"

# Health & Status
health: ## Check health of all services
	@echo "Checking health..."
	@docker-compose ps
	@echo ""
	@echo "Backend health:"
	@curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' || echo "❌ Backend not responding"
	@echo ""
	@echo "Frontend health:"
	@curl -s http://localhost:4200 > /dev/null && echo "✅ Frontend OK" || echo "❌ Frontend not responding"

ps: ## Show container status
	docker-compose ps

stats: ## Show container resource usage
	docker stats --no-stream

# Shell access
shell-backend: ## Shell into backend container
	docker exec -it immocare-backend sh

shell-frontend: ## Shell into frontend container
	docker exec -it immocare-frontend sh

shell-db: ## Shell into database container
	docker exec -it immocare-postgres sh

# Cleanup
prune: ## Remove unused Docker resources
	docker system prune -f
	@echo "✅ Unused Docker resources removed"

prune-all: ## Remove all unused Docker resources including volumes
	docker system prune -a --volumes -f
	@echo "✅ All unused Docker resources removed"
