# ImmoCare - Installation Guide

## Overview

This guide will help you set up the ImmoCare development environment on your local machine.

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Java JDK** | 17 (LTS) | Backend development |
| **Node.js** | 18+ | Frontend development |
| **npm** | 9+ | Package manager |
| **Maven** | 3.8+ | Build automation |
| **Docker Desktop** | Latest | Container runtime |
| **PostgreSQL** | 15+ | Database (via Docker) |
| **Git** | Latest | Version control |

### Optional Tools

- **IntelliJ IDEA** or **VS Code** - IDEs
- **Postman** or **Insomnia** - API testing
- **DBeaver** or **pgAdmin** - Database client

---

## Installation Steps

### 1. Install Java JDK 17

#### Windows
```bash
# Download from https://adoptium.net/
# Install and set JAVA_HOME environment variable
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.x"
setx PATH "%PATH%;%JAVA_HOME%\bin"
```

#### macOS
```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Linux
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

**Verify:**
```bash
java -version
# Should show: openjdk version "17.x.x"
```

---

### 2. Install Node.js and npm

#### Windows/macOS
Download from https://nodejs.org/ (LTS version)

#### Linux
```bash
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt install -y nodejs
```

**Verify:**
```bash
node --version  # Should be v18.x or higher
npm --version   # Should be 9.x or higher
```

---

### 3. Install Maven

#### Windows
Download from https://maven.apache.org/download.cgi
Extract and add to PATH

#### macOS
```bash
brew install maven
```

#### Linux
```bash
sudo apt install maven
```

**Verify:**
```bash
mvn --version
# Should show: Apache Maven 3.8.x or higher
```

---

### 4. Install Docker Desktop

Download and install from https://www.docker.com/products/docker-desktop

**Verify:**
```bash
docker --version
docker-compose --version
```

---

### 5. Clone the Repository

```bash
git clone https://github.com/your-org/immocare.git
cd immocare
```

---

## Project Setup

### 1. Database Setup (PostgreSQL with Docker)

#### Start PostgreSQL Container
```bash
docker-compose up -d postgres
```

This starts PostgreSQL on `localhost:5432` with:
- Database: `immocare`
- Username: `immocare_user`
- Password: `immocare_password`

#### Verify Database Connection
```bash
docker exec -it immocare-postgres psql -U immocare_user -d immocare
# Should connect successfully
# Type \q to exit
```

---

### 2. Backend Setup

#### Navigate to Backend Directory
```bash
cd code/backend
```

#### Configure Application Properties

Create `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/immocare
    username: immocare_user
    password: immocare_password
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: none  # Flyway handles schema
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

server:
  port: 8080

logging:
  level:
    com.immocare: DEBUG
    org.springframework.security: DEBUG
```

#### Install Dependencies
```bash
mvn clean install
```

#### Run Database Migrations
```bash
mvn flyway:migrate
```

This creates all database tables and inserts default admin user.

#### Run Backend Server
```bash
mvn spring-boot:run
```

Backend now running on `http://localhost:8080`

**Test:** 
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

---

### 3. Frontend Setup

#### Navigate to Frontend Directory
```bash
cd ../frontend
```

#### Install Dependencies
```bash
npm install
```

#### Configure Environment

Create `src/environments/environment.development.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

#### Run Frontend Development Server
```bash
npm start
# or
ng serve
```

Frontend now running on `http://localhost:4200`

---

## Default Credentials

After running Flyway migrations, a default admin user is created:

- **Username**: `admin`
- **Password**: `Admin123!`

**IMPORTANT:** Change this password immediately in production!

---

## Verify Installation

### 1. Check All Services
```bash
# PostgreSQL
docker ps | grep postgres

# Backend
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost:4200
```

### 2. Login to Application
1. Open browser: `http://localhost:4200`
2. Login with: `admin` / `Admin123!`
3. You should see the dashboard

### 3. Test API Endpoints
```bash
# Login (get token)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}'

# Get buildings (replace {token} with actual token)
curl http://localhost:8080/api/v1/buildings \
  -H "Authorization: Bearer {token}"
```

---

## Common Issues & Solutions

### Issue 1: Port Already in Use

**Error:** "Port 8080 is already in use"

**Solution:**
```bash
# Find process using port
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill process or change port in application.yml
```

---

### Issue 2: Database Connection Failed

**Error:** "Connection refused: localhost:5432"

**Solution:**
```bash
# Check if PostgreSQL container is running
docker ps

# Restart container
docker-compose restart postgres

# Check logs
docker logs immocare-postgres
```

---

### Issue 3: Flyway Migration Failed

**Error:** "Flyway migration failed"

**Solution:**
```bash
# Clean and retry
mvn flyway:clean
mvn flyway:migrate

# Or manually reset
docker exec -it immocare-postgres psql -U immocare_user -d immocare
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
\q

# Then run migrations again
mvn flyway:migrate
```

---

### Issue 4: NPM Install Fails

**Error:** "EACCES: permission denied"

**Solution:**
```bash
# Clear npm cache
npm cache clean --force

# Try again with clean install
rm -rf node_modules package-lock.json
npm install
```

---

## Development Workflow

### Daily Startup
```bash
# 1. Start database
docker-compose up -d postgres

# 2. Start backend (in code/backend)
mvn spring-boot:run

# 3. Start frontend (in code/frontend)
npm start

# 4. Open browser
open http://localhost:4200
```

### Daily Shutdown
```bash
# Stop frontend: Ctrl+C

# Stop backend: Ctrl+C

# Stop database
docker-compose down
```

---

## Running Tests

### Backend Tests
```bash
cd code/backend
mvn test
```

### Frontend Tests
```bash
cd code/frontend
npm test
```

### Integration Tests
```bash
cd code/backend
mvn verify
```

---

## Building for Production

### Backend
```bash
cd code/backend
mvn clean package -DskipTests
# Output: target/immocare-0.1.0.jar
```

### Frontend
```bash
cd code/frontend
npm run build --prod
# Output: dist/ folder
```

### Docker Images
```bash
# Build all images
docker-compose build

# Or individually
docker build -t immocare-backend:latest ./code/backend
docker build -t immocare-frontend:latest ./code/frontend
```

---

## Environment Variables

### Backend (.env or docker-compose.yml)
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=immocare
DB_USER=immocare_user
DB_PASSWORD=immocare_password

SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

JWT_SECRET=your-secret-key-change-in-production
JWT_EXPIRATION=86400000  # 24 hours
```

### Frontend (environment.ts)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

---

## IDE Setup

### IntelliJ IDEA (Backend)
1. File → Open → Select `code/backend`
2. Import as Maven project
3. Set JDK to Java 17
4. Enable annotation processing
5. Run configuration: Spring Boot → Main class: `com.immocare.ImmoCareApplication`

### VS Code (Frontend)
1. File → Open Folder → Select `code/frontend`
2. Install recommended extensions:
   - Angular Language Service
   - ESLint
   - Prettier
3. Reload window

---

## Database Management

### Access PostgreSQL CLI
```bash
docker exec -it immocare-postgres psql -U immocare_user -d immocare
```

### Common SQL Commands
```sql
-- List tables
\dt

-- Describe table
\d building

-- Query data
SELECT * FROM building;

-- Exit
\q
```

### Backup Database
```bash
docker exec immocare-postgres pg_dump -U immocare_user immocare > backup.sql
```

### Restore Database
```bash
docker exec -i immocare-postgres psql -U immocare_user immocare < backup.sql
```

---

## Next Steps

1. ✅ Installation complete
2. 📖 Read [CONTRIBUTING.md](./CONTRIBUTING.md) for development guidelines
3. 📝 Check [docs/analysis/](./docs/analysis/) for requirements
4. 🚀 Start coding!

---

## Getting Help

- **Documentation**: [docs/](./docs/)
- **Issues**: GitHub Issues
- **Wiki**: Project Wiki
- **Team Chat**: Slack/Teams channel

---

**Last Updated**: 2024-01-15  
**Version**: 1.0
