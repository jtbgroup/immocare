# ImmoCare - Makefile
SHELL := /bin/bash
# Variables
APP_NAME = immocare
BACKUP_DIR = ./backups
VERSION_FILE := VERSION
CURRENT_VERSION := $(shell cat $(VERSION_FILE))

.PHONY: help build up up-postgres down restart logs shell db-shell backup restore \
        clean clean-all rebuild dev prod health install update \
        backend-test nginx-reload nginx-test \
        start stop dev-build logs-dev \
		increment_version increment_beta release_version \
		seed-demo seed-cleandata-demo seed-real seed-cleandata-real

help: ## Affiche les commandes disponibles
	@echo "ImmoCare v$(VERSION) — Commandes disponibles :"
	@echo ""
	@echo "── Profils de base de données ───────────────────────────────────────────"
	@echo "  H2 (défaut)   : make up           → démarre sans PostgreSQL"
	@echo "  PostgreSQL    : make up-postgres   → démarre avec PostgreSQL"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-22s\033[0m %s\n", $$1, $$2}'

# ============================================
# VERSION MANAGEMENT
# ============================================
# 1. choose the increment and pass to beta.1
increment_version:
	@V=$(CURRENT_VERSION); \
	BASE=$${V%-*}; \
	MAJOR=$$(echo $$BASE | cut -d. -f1); \
	MINOR=$$(echo $$BASE | cut -d. -f2); \
	PATCH=$$(echo $$BASE | cut -d. -f3); \
	echo -e "Version actuelle : \033[1;32m$$V\033[0m"; \
	echo "Which segment do you want to increment ?"; \
	echo "1) Major ($$(($$MAJOR + 1)).0.0-beta.1)"; \
	echo "2) Minor ($$MAJOR.$$(($$MINOR + 1)).0-beta.1)"; \
	echo "3) Patch ($$MAJOR.$$MINOR.$$(($$PATCH + 1))-beta.1)"; \
	read -p "Your choice (1-3) : " choice; \
	case $$choice in \
		1) NEXT="$$(($$MAJOR + 1)).0.0-beta.1" ;; \
		2) NEXT="$$MAJOR.$$(($$MINOR + 1)).0-beta.1" ;; \
		3) NEXT="$$MAJOR.$$MINOR.$$(($$PATCH + 1))-beta.1" ;; \
		*) echo "❌ Invalid choice"; exit 1 ;; \
	esac; \
	$(MAKE) set-version V=$$NEXT; \
	$(MAKE) git-tag V=$$NEXT

# 2. Increment the beta (ex: -beta.1 -> -beta.2)
increment_beta:
	@V=$(CURRENT_VERSION); \
	if [[ "$$V" != *"-beta."* ]]; then \
		echo "❌ Error: Version $$V is not a beta."; exit 1; \
	fi; \
	BASE=$${V%-beta.*}; \
	NUM=$${V##*-beta.}; \
	NEXT="$$BASE-beta.$$(($$NUM + 1))"; \
	$(MAKE) set-version V=$$NEXT; \
	$(MAKE) git-tag V=$$NEXT

# 3. Final release (removes the beta suffix)
release_version:
	@V=$(CURRENT_VERSION); \
	if [[ "$$V" != *"-beta."* ]]; then \
		echo "❌ Error: No beta version to release."; exit 1; \
	fi; \
	NEXT=$${V%-beta.*}; \
	echo "--- ATTENTION ---"; \
	echo -e "You are about to release the stable version : \033[1;31m$$NEXT\033[0m"; \
	read -p "Confirm the release ? (y/n) " ans; \
	if [ "$$ans" != "y" ]; then echo "Cancelled."; exit 1; fi; \
	$(MAKE) set-version V=$$NEXT; \
	$(MAKE) git-tag V=$$NEXT

set-version:
	@echo "$(V)" > $(VERSION_FILE)
	@mvn -f backend/pom.xml versions:set -DnewVersion=$(V) -DgenerateBackupPoms=false -q 2>/dev/null || echo "⚠️ Maven skipped"
	@cd frontend && npm version $(V) --no-git-tag-version --allow-same-version > /dev/null 2>&1 || echo "⚠️ npm skipped"
	@echo "✅ Files updated to $(V)"

git-tag:
	@git add .
	@git commit -m "chore: bump version to $(V)"
	@git tag -a v$(V) -m "Release v$(V)"
	@git push origin $$(git rev-parse --abbrev-ref HEAD)
	@git push origin v$(V)

# ── Production — H2 (défaut) ──────────────────────────────────────────────────

build: ## Build l'image Docker
	@echo "Build $(APP_NAME):$(CURRENT_VERSION)..."
	docker compose build

up: ## ▶  Démarrer avec H2 (base embarquée, recommandé)
	@echo "Démarrage avec H2..."
	docker compose up -d
	@echo "✓ Services démarrés (H2)"
	@echo "  Application : http://localhost:8090"
	@echo "  Health      : http://localhost:8090/actuator/health"

# ── Production — PostgreSQL ───────────────────────────────────────────────────

up-postgres: ## ▶  Démarrer avec PostgreSQL (--profile postgres)
	@echo "Démarrage avec PostgreSQL..."
	DB_PROFILE=postgres docker compose --profile postgres up -d
	@echo "✓ Services démarrés (PostgreSQL)"
	@echo "  Application : http://localhost:8090"
	@echo "  Health      : http://localhost:8090/actuator/health"

down: ## ⏹  Arrêter tous les services
	@echo "Arrêt des services..."
	docker compose --profile postgres down
	@echo "✓ Services arrêtés"

restart: ## 🔄 Redémarrer les services
	docker compose restart
	@echo "✓ Services redémarrés"

prod: ## Build et démarrer en production (H2)
	docker compose up -d --build
	@echo "✓ Déploiement production (H2) terminé"
	@echo "  Application : http://localhost:8090"

prod-postgres: ## Build et démarrer en production (PostgreSQL)
	DB_PROFILE=postgres docker compose --profile postgres up -d --build
	@echo "✓ Déploiement production (PostgreSQL) terminé"
	@echo "  Application : http://localhost:8090"

rebuild: ## Build complet sans cache
	@echo "Rebuild depuis zéro..."
	docker compose build --no-cache
	@echo "✓ Rebuild terminé"

# ── Développement (usage quotidien) ──────────────────────────────────────────


dev-down:
	@echo "⛔ Stopping development environment..."
	docker compose -f docker-compose.dev.yml down
	@echo "✅ Development services stopped"

dev-logs:
	docker compose -f docker-compose.dev.yml logs -f

dev-clean:
	@echo "🧹 Cleaning development environment (removes volumes and orphan containers)..."
	docker compose -f docker-compose.dev.yml --profile postgres down -v --remove-orphans
	@docker volume rm immocare-dev_maven_cache immocare-dev_node_modules immocare-dev_postgres_data_dev \
		immocare_maven_cache immocare_node_modules immocare_postgres_data_dev 2>/dev/null || true
	@echo "✅ Development environment cleaned"


dev-start: ## ▶  Démarrer l'env de dev avec PostgreSQL (sans rebuild)
	@echo "Démarrage env de dev (PostgreSQL)..."
	DB_PROFILE=postgres docker compose -f docker-compose.dev.yml up -d
	@echo ""
	@echo "✅ Development services (postgres) started!"
	@echo "   🌐 App (Nginx):       http://localhost:8080"
	@echo "   🎨 Angular direct:    http://localhost:4200"
	@echo "   🔧 Backend direct:    http://localhost:8081"
	@echo "   🐛 Remote Debug:      localhost:5005"
	@echo ""
	@echo "📋 View logs: make dev-logs"

dev-clean-start:
	make dev-clean
	make dev-start

# ── Logs ─────────────────────────────────────────────────────────────────────

logs: ## Voir tous les logs
	docker compose logs -f

logs-app: ## Voir les logs de l'application
	docker compose logs -f app

logs-db: ## Voir les logs de la base de données
	docker compose logs -f postgres

# ── Base de données ───────────────────────────────────────────────────────────

db-shell: ## Ouvrir un shell PostgreSQL (requiert --profile postgres)
	docker compose --profile postgres exec postgres psql -U immocare -d immocare

h2-console: ## Afficher l'URL de la console H2
	@echo "Console H2 : http://localhost:8080/h2-console"
	@echo "  JDBC URL : jdbc:h2:file:./data/immocare"
	@echo "  User     : sa"
	@echo "  Password : (vide)"

backup: ## Sauvegarder la base PostgreSQL
	@mkdir -p $(BACKUP_DIR)
	@echo "Création du backup..."
	@docker compose --profile postgres exec -T postgres pg_dump -U immocare immocare \
		> $(BACKUP_DIR)/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "✓ Backup créé dans $(BACKUP_DIR)"

restore: ## Restaurer la base PostgreSQL (usage: make restore FILE=backup.sql)
	@if [ -z "$(FILE)" ]; then \
		echo "Erreur : spécifiez FILE=backup.sql"; \
		exit 1; \
	fi
	@echo "Restauration depuis $(FILE)..."
	@docker compose --profile postgres exec -T postgres psql -U immocare immocare < $(FILE)
	@echo "✓ Backup restauré"

# ── Tests ─────────────────────────────────────────────────────────────────────

backend-test: ## Lancer les tests backend (utilise H2 en mémoire)
	cd backend && mvn test
	@echo "✓ Tests backend terminés"

# ── Accès aux shells ──────────────────────────────────────────────────────────

shell: ## Ouvrir un shell dans le conteneur app
	docker compose exec app sh

shell-db: ## Ouvrir un shell dans le conteneur postgres
	docker compose --profile postgres exec postgres sh

# ── Nginx ─────────────────────────────────────────────────────────────────────

nginx-reload: ## Recharger la configuration nginx
	docker compose exec app nginx -s reload
	@echo "✓ Configuration nginx rechargée"

nginx-test: ## Tester la configuration nginx
	docker compose exec app nginx -t

# ── Santé & Statut ────────────────────────────────────────────────────────────

status: ## Voir l'état des services
	docker compose ps

health: ## Vérifier la santé des services
	@echo "Vérification de la santé des services..."
	@docker compose ps
	@echo ""
	@curl -s -o /dev/null -w "App health : HTTP %{http_code}\n" \
		http://localhost:8090/actuator/health || echo "App: ne répond pas"

monitor: ## Voir l'utilisation des ressources
	docker stats immocare-app

# ── Nettoyage ─────────────────────────────────────────────────────────────────

clean: ## Supprimer les ressources Docker inutilisées
	@echo "Nettoyage..."
	docker system prune -f
	@echo "✓ Nettoyage terminé"

clean-all: ## Tout supprimer, y compris les volumes ⚠️  supprime toutes les données
	@echo "⚠️  ATTENTION : toutes les données seront supprimées !"
	@read -p "Continuer ? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker compose --profile postgres down -v; \
		docker compose -f docker-compose.dev.yml --profile postgres down -v; \
		docker system prune -a -f; \
		echo "✓ Nettoyage complet terminé"; \
	fi

# ── Installation ──────────────────────────────────────────────────────────────

install: ## Installation initiale complète (H2)
	@echo "=== Installation ImmoCare ==="
	@echo "1. Build des images..."
	@$(MAKE) build
	@echo "2. Démarrage des services (H2)..."
	@$(MAKE) up
	@echo "3. Attente que les services soient prêts..."
	@sleep 15
	@echo "4. Vérification de la santé..."
	@$(MAKE) health
	@echo ""
	@echo "✓ Installation terminée !"
	@echo "  Application : http://localhost:8090"
	@echo "  Identifiants par défaut : à configurer (H2 repart vierge)"
	@echo ""
	@echo "Pour utiliser PostgreSQL : make install-postgres"

install-postgres: ## Installation initiale avec PostgreSQL
	@echo "=== Installation ImmoCare (PostgreSQL) ==="
	@echo "1. Build des images..."
	@$(MAKE) build
	@echo "2. Démarrage des services (PostgreSQL)..."
	@$(MAKE) up-postgres
	@echo "3. Attente que les services soient prêts..."
	@sleep 20
	@echo "4. Vérification de la santé..."
	@$(MAKE) health
	@echo ""
	@echo "✓ Installation terminée !"
	@echo "  Application : http://localhost:8090"
	@echo "  Identifiants : admin / Admin1234!"

update: ## Mettre à jour l'application (pull + rebuild + redémarrer)
	@echo "Mise à jour..."
	@git pull
	@$(MAKE) down
	@$(MAKE) build
	@$(MAKE) up
	@echo "✓ Mise à jour terminée"


# ============================================
# SEED
# ============================================
# Usage:
#   make seed-demo                   → demo data, dev (localhost:8081)
#   make seed-real                   → real data, prod (localhost:8090)

reset-postgres: ## 🗑  Reset via SQL direct (requiert psql + PostgreSQL)
	@echo "🌱 Resetting data"
	@chmod +x scripts/reset-sql.sh
	@scripts/reset-sql.sh \
		$${DB_HOST:-localhost} \
		$${DB_PORT:-5432} \
		$${DB_NAME:-immocare} \
		$${DB_USER:-immocare} \
		$${DB_PASS:-immocare} \
		$${ADMIN:-admin}

seed-demo: ## 🌱 Seed demo data (dev)
	@echo "🌱 Seeding demo data"
	@chmod +x scripts/seed.sh
	@scripts/seed.sh http://localhost:8081 admin admin123 scripts/demo-data


seed-real: ## 🌱 Seed real data (prod)
	@echo "🌱 Seeding real data"
	@chmod +x scripts/seed.sh
	@scripts/seed.sh $${URL:-http://localhost:8081} admin admin123 scripts/real-data

seed-cleandata-real: ## 🌱 Seed real data after reset (dev)
	@$(MAKE) reset-postgres
	@$(MAKE) seed-real

seed-cleandata-demo: ## 🌱 Seed demo data after reset (dev)
	@$(MAKE) reset-postgres
	@$(MAKE) seed-demo