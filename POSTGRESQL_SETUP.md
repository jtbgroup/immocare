# Guide de Configuration PostgreSQL

## Option 1 : Installation PostgreSQL Locale (Recommandé pour Production)

### Ubuntu/Debian
```bash
# Installer PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib

# Démarrer le service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Se connecter en tant que postgres
sudo -u postgres psql

# Dans psql, créer la base et l'utilisateur
CREATE DATABASE immocare;
CREATE USER immocare WITH PASSWORD 'immocare';
GRANT ALL PRIVILEGES ON DATABASE immocare TO immocare;
\q
```

### macOS (avec Homebrew)
```bash
# Installer PostgreSQL
brew install postgresql@15

# Démarrer le service
brew services start postgresql@15

# Créer la base et l'utilisateur
createdb immocare
createuser immocare
psql -c "ALTER USER immocare WITH PASSWORD 'immocare';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE immocare TO immocare;"
```

### Windows
1. Télécharger PostgreSQL depuis https://www.postgresql.org/download/windows/
2. Installer avec pgAdmin
3. Créer la base de données "immocare"
4. Créer l'utilisateur "immocare" avec le mot de passe "immocare"

---

## Option 2 : Docker (Recommandé pour Développement)

### Utiliser Docker Compose

Créez un fichier `docker-compose.yml` à la racine du projet :

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: immocare-postgres
    environment:
      POSTGRES_DB: immocare
      POSTGRES_USER: immocare
      POSTGRES_PASSWORD: immocare
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U immocare"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres-data:
```

### Démarrer PostgreSQL avec Docker Compose

```bash
# Démarrer PostgreSQL
docker-compose up -d

# Vérifier que le conteneur est démarré
docker-compose ps

# Voir les logs
docker-compose logs -f postgres

# Arrêter PostgreSQL
docker-compose down

# Arrêter et supprimer les données
docker-compose down -v
```

### Utiliser Docker directement

```bash
# Démarrer PostgreSQL
docker run --name immocare-postgres \
  -e POSTGRES_DB=immocare \
  -e POSTGRES_USER=immocare \
  -e POSTGRES_PASSWORD=immocare \
  -p 5432:5432 \
  -d postgres:15-alpine

# Vérifier que le conteneur est démarré
docker ps

# Voir les logs
docker logs -f immocare-postgres

# Arrêter PostgreSQL
docker stop immocare-postgres

# Redémarrer PostgreSQL
docker start immocare-postgres

# Supprimer le conteneur
docker rm -f immocare-postgres
```

---

## Option 3 : Tests Sans PostgreSQL

Si vous voulez juste **lancer les tests** sans installer PostgreSQL :

```bash
# Les tests utilisent H2 en mémoire
cd backend
mvn test

# Tous les tests passent sans PostgreSQL ! ✅
```

Les tests utilisent automatiquement H2 grâce à `application-test.properties`.

---

## Vérifier la Connexion

### Avec psql
```bash
psql -h localhost -U immocare -d immocare
# Mot de passe: immocare

# Dans psql:
\dt  # Lister les tables (vide au début)
\q   # Quitter
```

### Avec un client GUI
- **pgAdmin** : https://www.pgadmin.org/
- **DBeaver** : https://dbeaver.io/
- **DataGrip** : https://www.jetbrains.com/datagrip/

Configuration :
- Host: localhost
- Port: 5432
- Database: immocare
- User: immocare
- Password: immocare

---

## Lancer l'Application

Une fois PostgreSQL démarré :

```bash
cd backend
mvn spring-boot:run
```

Au premier démarrage, Flyway va :
1. ✅ Créer les tables (user_account, building)
2. ✅ Insérer l'utilisateur admin par défaut

Vérifier dans la base :
```sql
SELECT * FROM flyway_schema_history;  -- Migrations appliquées
SELECT * FROM user_account;            -- Admin créé
SELECT * FROM building;                -- Vide au début
```

---

## Troubleshooting

### Erreur : "Connection refused"
```
Connection to localhost:5432 refused
```

**Solutions :**
1. PostgreSQL n'est pas démarré
   ```bash
   # Ubuntu/Debian
   sudo systemctl start postgresql
   
   # macOS
   brew services start postgresql@15
   
   # Docker
   docker-compose up -d
   ```

2. Vérifier que PostgreSQL écoute sur le port 5432
   ```bash
   netstat -an | grep 5432
   # ou
   lsof -i :5432
   ```

### Erreur : "password authentication failed"
```
FATAL: password authentication failed for user "immocare"
```

**Solution :** Réinitialiser le mot de passe
```bash
sudo -u postgres psql
ALTER USER immocare WITH PASSWORD 'immocare';
\q
```

### Erreur : "database does not exist"
```
FATAL: database "immocare" does not exist
```

**Solution :** Créer la base de données
```bash
sudo -u postgres createdb immocare
# ou
psql -U postgres -c "CREATE DATABASE immocare;"
```

### Les tests échouent à cause de PostgreSQL

**Solution :** Les tests ne devraient PAS utiliser PostgreSQL.

Vérifiez que `src/test/resources/application-test.properties` existe et contient :
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.flyway.enabled=false
```

---

## Résumé des Commandes

### Développement avec Docker (Recommandé)
```bash
# 1. Démarrer PostgreSQL
docker-compose up -d

# 2. Lancer le backend
cd backend
mvn spring-boot:run

# 3. Lancer le frontend (dans un autre terminal)
cd frontend
npm install
npm start

# 4. Accéder à l'application
# Backend: http://localhost:8080
# Frontend: http://localhost:4200
```

### Tests
```bash
# Tests backend (H2 en mémoire, pas besoin de PostgreSQL)
cd backend
mvn test

# Tests frontend
cd frontend
npm test
```

---

## Prochaines Étapes

Une fois PostgreSQL configuré et l'application démarrée :

1. ✅ Vérifier que l'admin existe
   ```sql
   SELECT * FROM user_account;
   ```

2. ✅ Créer un building via l'API
   ```bash
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

3. ✅ Vérifier dans la base
   ```sql
   SELECT * FROM building;
   ```

4. ✅ Accéder au frontend
   - Ouvrir http://localhost:4200
   - Voir la liste des buildings
   - Créer un nouveau building

---

**Besoin d'aide ?** Consultez les logs :
```bash
# Backend
tail -f backend/logs/spring.log

# PostgreSQL (Docker)
docker logs -f immocare-postgres
```
