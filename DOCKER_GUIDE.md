# Guide Docker - ImmoCare

## 🐳 Architecture Docker

```
┌─────────────────────────────────────────────────┐
│              Docker Network                      │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │          │  │          │  │          │      │
│  │ Frontend │──│ Backend  │──│PostgreSQL│      │
│  │ (Nginx)  │  │ (Spring) │  │          │      │
│  │          │  │          │  │          │      │
│  │ Port 80  │  │Port 8080 │  │Port 5432 │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│       ↓             ↓              ↓            │
│   Volume      Volume         Volume             │
│   (built)     (jar)          (data)             │
└─────────────────────────────────────────────────┘
        ↓               ↓              ↓
    localhost:     localhost:     localhost:
      4200            8080           5432
```

---

## 🚀 Démarrage Rapide

### Option 1 : Production (recommandé pour tests)

```bash
# 1. Construire et démarrer tous les containers
docker-compose up -d

# 2. Voir les logs
docker-compose logs -f

# 3. Vérifier le statut
docker-compose ps

# 4. Accéder à l'application
# Frontend: http://localhost:4200
# Backend API: http://localhost:8080
# Health check: http://localhost:8080/actuator/health
```

### Option 2 : Développement (hot reload)

```bash
# 1. Démarrer en mode développement
docker-compose -f docker-compose.dev.yml up

# 2. Les changements de code sont automatiquement détectés !
# - Backend: Spring Boot DevTools recharge automatiquement
# - Frontend: Angular CLI recompile automatiquement

# 3. Debugger le backend (VS Code)
# Ouvrir VS Code > Run > "Debug Backend (Docker)"
```

---

## 📋 Commandes Utiles

### Gestion des Containers

```bash
# Démarrer tous les services
docker-compose up -d

# Démarrer en mode développement
docker-compose -f docker-compose.dev.yml up

# Arrêter tous les services
docker-compose down

# Arrêter et supprimer les volumes (⚠️ efface la base de données)
docker-compose down -v

# Redémarrer un service spécifique
docker-compose restart backend

# Rebuild un service
docker-compose up -d --build backend
```

### Logs et Monitoring

```bash
# Voir tous les logs
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres

# Voir les 100 dernières lignes
docker-compose logs --tail=100 backend

# Voir le statut des services
docker-compose ps

# Voir les ressources utilisées
docker stats
```

### Accès aux Containers

```bash
# Shell dans le backend
docker exec -it immocare-backend sh

# Shell dans le frontend
docker exec -it immocare-frontend sh

# psql dans PostgreSQL
docker exec -it immocare-postgres psql -U immocare -d immocare

# Exécuter une commande Maven
docker exec -it immocare-backend mvn test
```

### Nettoyage

```bash
# Supprimer tous les containers arrêtés
docker container prune

# Supprimer toutes les images inutilisées
docker image prune -a

# Supprimer tous les volumes inutilisés
docker volume prune

# Nettoyage complet (⚠️ attention)
docker system prune -a --volumes
```

---

## 🔧 Utilisation depuis VS Code

### Méthode 1 : Docker Extension (GUI)

1. **Installer l'extension Docker** (si pas déjà fait)
   - Ouvrir Extensions (Ctrl+Shift+X)
   - Chercher "Docker" par Microsoft
   - Installer

2. **Utiliser la sidebar Docker**
   - Clic droit sur `docker-compose.yml` → "Compose Up"
   - Voir les containers en cours dans la sidebar
   - Clic droit sur un container → View Logs / Stop / Restart

### Méthode 2 : Tasks VS Code (Recommandé)

1. **Ouvrir la palette de commandes** (Ctrl+Shift+P)
2. Taper "Run Task"
3. Choisir une tâche :
   - `Docker: Start All (Production)`
   - `Docker: Start All (Development)` ⭐
   - `Docker: Stop All`
   - `Docker: Rebuild All`
   - `Docker: View Logs`
   - `Docker: Clean All`

### Méthode 3 : Terminal Intégré

1. **Ouvrir le terminal** (Ctrl+`)
2. Lancer les commandes Docker directement

### Debugging

1. **Démarrer en mode développement**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **Attacher le debugger**
   - Aller dans "Run and Debug" (Ctrl+Shift+D)
   - Sélectionner "Debug Backend (Docker)"
   - Appuyer sur F5

3. **Mettre des breakpoints** dans votre code Java
   - Le debugger s'arrête sur les breakpoints !

---

## 📂 Structure des Fichiers Docker

```
immocare/
├── docker-compose.yml           # Production (optimisé, images minimales)
├── docker-compose.dev.yml       # Développement (hot reload, debug)
│
├── backend/
│   ├── Dockerfile               # Production (multi-stage build)
│   ├── Dockerfile.dev           # Développement (Maven hot reload)
│   └── .dockerignore            # Fichiers ignorés
│
├── frontend/
│   ├── Dockerfile               # Production (Nginx)
│   ├── Dockerfile.dev           # Développement (ng serve)
│   ├── nginx.conf               # Configuration Nginx
│   └── .dockerignore            # Fichiers ignorés
│
└── .vscode/
    ├── tasks.json               # Tâches Docker
    ├── launch.json              # Configurations debug
    ├── extensions.json          # Extensions recommandées
    └── settings.json            # Paramètres workspace
```

---

## 🔍 Vérifications

### 1. Vérifier que tous les containers sont démarrés

```bash
docker-compose ps
```

Résultat attendu :
```
NAME                    STATUS              PORTS
immocare-postgres       Up (healthy)        5432
immocare-backend        Up (healthy)        8080, 5005
immocare-frontend       Up (healthy)        4200
```

### 2. Vérifier les health checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost:4200

# PostgreSQL
docker exec immocare-postgres pg_isready -U immocare
```

### 3. Vérifier les logs

```bash
# Aucune erreur dans les logs
docker-compose logs | grep -i error
```

### 4. Tester l'API

```bash
# Lister les buildings
curl http://localhost:8080/api/v1/buildings

# Créer un building
curl -X POST http://localhost:8080/api/v1/buildings \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Building",
    "streetAddress": "123 Main St",
    "postalCode": "1000",
    "city": "Brussels",
    "country": "Belgium"
  }'
```

---

## 🐛 Troubleshooting

### Problème : Port déjà utilisé

```
Error: bind: address already in use
```

**Solution 1** : Arrêter le processus qui utilise le port
```bash
# Trouver le processus
lsof -i :8080  # ou :4200, :5432

# Tuer le processus
kill -9 <PID>
```

**Solution 2** : Changer le port dans docker-compose.yml
```yaml
ports:
  - "8081:8080"  # Utiliser 8081 au lieu de 8080
```

### Problème : Container ne démarre pas

```bash
# Voir les logs détaillés
docker-compose logs backend

# Reconstruire l'image
docker-compose up -d --build backend

# Vérifier la santé
docker inspect immocare-backend | grep Health
```

### Problème : Backend ne se connecte pas à PostgreSQL

```bash
# Vérifier que postgres est healthy
docker-compose ps

# Tester la connexion manuellement
docker exec -it immocare-backend sh
wget postgres:5432
```

### Problème : Frontend ne charge pas

```bash
# Vérifier les logs Nginx
docker-compose logs frontend

# Vérifier que le build a réussi
docker exec -it immocare-frontend ls /usr/share/nginx/html

# Reconstruire
docker-compose up -d --build frontend
```

### Problème : Images prennent trop de place

```bash
# Voir l'espace utilisé
docker system df

# Nettoyer
docker system prune -a
```

---

## 🎯 Workflows de Développement

### Workflow 1 : Développement Frontend uniquement

```bash
# Démarrer uniquement backend + database
docker-compose up -d postgres backend

# Lancer frontend localement (plus rapide)
cd frontend
npm install
npm start

# Frontend sur http://localhost:4200
# API sur http://localhost:8080
```

### Workflow 2 : Développement Backend uniquement

```bash
# Démarrer uniquement database
docker-compose up -d postgres

# Lancer backend localement
cd backend
mvn spring-boot:run

# Frontend en Docker
docker-compose up -d frontend
```

### Workflow 3 : Full Stack Docker

```bash
# Tout en Docker avec hot reload
docker-compose -f docker-compose.dev.yml up

# Modifier le code → automatiquement rechargé !
```

---

## 📊 Monitoring et Métriques

### Spring Boot Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Info
curl http://localhost:8080/actuator/info

# Métriques (si activées)
curl http://localhost:8080/actuator/metrics
```

### Docker Stats

```bash
# Voir CPU, RAM, Network en temps réel
docker stats

# Format personnalisé
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
```

---

## 🔐 Sécurité

### Production Checklist

- [ ] Changer les mots de passe par défaut (PostgreSQL)
- [ ] Utiliser des secrets Docker au lieu de variables d'environnement
- [ ] Activer HTTPS (avec certificats SSL)
- [ ] Restreindre les CORS origins
- [ ] Utiliser des images officielles et scannées
- [ ] Ne pas exposer les ports de debug (5005)
- [ ] Mettre à jour régulièrement les images base

### Scan de Vulnérabilités

```bash
# Scanner une image
docker scan immocare-backend

# Avec Trivy (alternative)
trivy image immocare-backend:latest
```

---

## 📚 Ressources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Angular Docker Guide](https://angular.io/guide/deployment#docker)
- [VS Code Docker Extension](https://code.visualstudio.com/docs/containers/overview)

---

## 💡 Tips & Tricks

### 1. Rebuild rapide

```bash
# Ne rebuild que ce qui a changé
docker-compose up -d --build --no-deps backend
```

### 2. Logs colorés

```bash
# Installer grc (Generic Colouriser)
# Ubuntu/Debian: apt install grc
# Mac: brew install grc

grc docker-compose logs -f
```

### 3. Alias utiles

Ajoutez dans votre `.bashrc` ou `.zshrc` :

```bash
alias dcup='docker-compose up -d'
alias dcdown='docker-compose down'
alias dclogs='docker-compose logs -f'
alias dcps='docker-compose ps'
alias dcrestart='docker-compose restart'
```

### 4. Watch mode

```bash
# Relancer automatiquement au changement
watch -n 2 docker-compose ps
```

---

**Dernière mise à jour**: 2024-01-15  
**Version**: 2.0.0 - Full Docker Support
