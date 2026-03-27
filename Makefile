# ImmoCare - Makefile
.PHONY: help build up up-postgres down restart logs shell db-shell backup restore \
        clean clean-all rebuild dev prod health install update \
        backend-test nginx-reload nginx-test \
        start stop dev-build logs-dev

# Variables
APP_NAME = immocare
VERSION  = 1.0
BACKUP_DIR = ./backups

help: ## Affiche les commandes disponibles
	@echo "ImmoCare v$(VERSION) — Commandes disponibles :"
	@echo ""
	@echo "── Profils de base de données ───────────────────────────────────────────"
	@echo "  H2 (défaut)   : make up           → démarre sans PostgreSQL"
	@echo "  PostgreSQL    : make up-postgres   → démarre avec PostgreSQL"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-22s\033[0m %s\n", $$1, $$2}'

# ── Production — H2 (défaut) ──────────────────────────────────────────────────

build: ## Build l'image Docker
	@echo "Build $(APP_NAME):$(VERSION)..."
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

start: ## ▶  Démarrer l'env de dev avec H2 (sans rebuild)
	@echo "Démarrage env de dev (H2)..."
	docker compose -f docker-compose.dev.yml up -d
	@echo "✓ Dev démarré (H2, sans rebuild)"
	@echo "  Frontend : http://localhost:4200"
	@echo "  Backend  : http://localhost:8080"

start-postgres: ## ▶  Démarrer l'env de dev avec PostgreSQL (sans rebuild)
	@echo "Démarrage env de dev (PostgreSQL)..."
	DB_PROFILE=postgres docker compose -f docker-compose.dev.yml --profile postgres up -d
	@echo "✓ Dev démarré (PostgreSQL, sans rebuild)"
	@echo "  Frontend : http://localhost:4200"
	@echo "  Backend  : http://localhost:8080"

stop: ## ⏹  Arrêter l'env de dev
	@echo "Arrêt env de dev..."
	docker compose -f docker-compose.dev.yml --profile postgres down
	@echo "✓ Dev arrêté"

dev-build: ## 🔨 Rebuild les images de dev (après changement pom.xml / package.json)
	@echo "Rebuild images dev (H2)..."
	docker compose -f docker-compose.dev.yml up -d --build
	@echo "✓ Images dev rebuildées"
	@echo "  Frontend : http://localhost:4200"
	@echo "  Backend  : http://localhost:8080"

dev-build-postgres: ## 🔨 Rebuild les images de dev (PostgreSQL)
	@echo "Rebuild images dev (PostgreSQL)..."
	DB_PROFILE=postgres docker compose -f docker-compose.dev.yml --profile postgres up -d --build
	@echo "✓ Images dev rebuildées (PostgreSQL)"

logs-dev: ## Voir les logs de dev
	docker compose -f docker-compose.dev.yml logs -f

# ── Commandes legacy (compatibilité) ─────────────────────────────────────────

dev: ## Démarrer en mode dev avec logs visibles (rebuild — utiliser 'start' au quotidien)
	docker compose -f docker-compose.dev.yml up --build

dev-d: ## Démarrer en mode dev détaché (rebuild — utiliser 'start' au quotidien)
	docker compose -f docker-compose.dev.yml up -d --build
	@echo "✓ Services dev démarrés"
	@echo "  Frontend : http://localhost:4200"
	@echo "  Backend  : http://localhost:8080"

dev-down: ## Arrêter les services de développement
	docker compose -f docker-compose.dev.yml down
	@echo "✓ Services dev arrêtés"

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