# ImmoCare - Property Management System

Full-stack application for managing buildings, housing units, and rental operations.

**Stack**: Spring Boot 3.2 + Angular 17 + PostgreSQL 15 + Docker

---

## 🚀 Quick Start (Docker - Recommended)

### Prerequisites
- Docker Desktop installed
- VS Code (recommended)

### Start Everything with One Command

```bash
# Clone the repository
git clone <your-repo>
cd immocare

# Start all services (PostgreSQL + Backend + Frontend)
docker compose up -d

# Or use make (if available)
make up
```

**That's it!** 🎉

- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health

### Stop Everything

```bash
docker compose down

# Or
make down
```

---

## 📋 What's Running?

| Service | Container | Port | URL |
|---------|-----------|------|-----|
| Frontend | immocare-frontend | 4200 | http://localhost:4200 |
| Backend | immocare-backend | 8080 | http://localhost:8080 |
| Database | immocare-postgres | 5432 | jdbc:postgresql://localhost:5432/immocare |

---

## 🔧 Development Mode (Hot Reload)

```bash
# Start with automatic code reload
docker compose -f docker compose.dev.yml up

# Or
make dev
```

**Changes are automatically detected:**
- ✅ Backend: Spring Boot DevTools reloads on Java file changes
- ✅ Frontend: Angular CLI recompiles on TypeScript/HTML/CSS changes
- ✅ Database: Data persists between restarts

---

## 💻 VS Code Integration

### Recommended Extensions

Open VS Code and install recommended extensions:
- Docker (ms-azuretools.vscode-docker)
- Spring Boot Dashboard
- Angular Language Service

Extensions are auto-suggested when you open the project.

### Quick Commands

**Press `Ctrl+Shift+P` and type:**

- `Tasks: Run Task` → Choose:
  - `Docker: Start All (Development)` ⭐
  - `Docker: Start All (Production)`
  - `Docker: Stop All`
  - `Docker: View Logs`
  
**Or use keyboard shortcuts:**
- `Ctrl+Shift+B` → Default build task (starts dev mode)

### Debugging

1. **Start development mode**:
   ```bash
   docker compose -f docker compose.dev.yml up -d
   ```

2. **Start debugging**:
   - Press `F5`
   - Or: Run > "Debug Backend (Docker)"

3. **Set breakpoints** in your Java code
   - Debugger will pause on breakpoints!

---

## 🛠️ Common Commands

### Using Make (Recommended)

```bash
make help          # Show all available commands
make up            # Start (production)
make dev           # Start (development with hot reload)
make down          # Stop
make logs          # View logs
make restart       # Restart all
make rebuild       # Rebuild images
make test          # Run tests
make db-shell      # Open PostgreSQL shell
make health        # Check service health
```

### Using Docker Compose

```bash
# Production mode
docker compose up -d                    # Start
docker compose down                     # Stop
docker compose logs -f                  # View logs
docker compose restart                  # Restart
docker compose up -d --build            # Rebuild

# Development mode
docker compose -f docker compose.dev.yml up    # Start dev
docker compose -f docker compose.dev.yml down  # Stop dev
```

### Individual Services

```bash
# Restart just one service
docker compose restart backend

# Rebuild just one service
docker compose up -d --build frontend

# View logs of one service
docker compose logs -f backend
```

---

## 📊 Monitoring

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

### Check Status

```bash
docker compose ps

# Or with make
make ps
make health
make stats
```

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost:4200

# Database
docker exec immocare-postgres pg_isready -U immocare
```

---

## 🧪 Testing

### Run All Tests

```bash
make test

# Or manually
cd backend && mvn test
cd frontend && npm test
```

Tests run on **H2 in-memory database**, so PostgreSQL is not required for testing.

---

## 🗄️ Database Access

### Using psql

```bash
# Open PostgreSQL shell
make db-shell

# Or directly
docker exec -it immocare-postgres psql -U immocare -d immocare
```

### Using GUI Client

Connect with DBeaver, pgAdmin, or any PostgreSQL client:

```
Host:     localhost
Port:     5432
Database: immocare
User:     immocare
Password: immocare
```

### Default Admin User

```
Username: admin
Password: admin123
Email:    admin@immocare.com
```

---

## 🐛 Troubleshooting

### Port already in use

```bash
# Find and kill process using port 8080
lsof -i :8080
kill -9 <PID>

# Or change port in docker compose.yml
```

### Container won't start

```bash
# View detailed logs
docker compose logs backend

# Rebuild image
docker compose up -d --build backend
```

### Database connection issues

```bash
# Check postgres is healthy
docker compose ps

# Restart database
docker compose restart postgres
```

### Clear everything and start fresh

```bash
# ⚠️ This will delete all data
docker compose down -v
docker compose up -d

# Or
make clean
make up
```

---

## 📁 Project Structure

```
immocare/
├── backend/                    # Spring Boot backend
│   ├── src/
│   ├── Dockerfile              # Production image
│   ├── Dockerfile.dev          # Development image
│   └── pom.xml
│
├── frontend/                   # Angular frontend
│   ├── src/
│   ├── Dockerfile              # Production image (Nginx)
│   ├── Dockerfile.dev          # Development image (ng serve)
│   └── package.json
│
├── docker compose.yml          # Production orchestration
├── docker compose.dev.yml      # Development orchestration
├── Makefile                    # Simplified commands
│
├── .vscode/                    # VS Code configuration
│   ├── tasks.json              # Custom tasks
│   ├── launch.json             # Debug configs
│   └── extensions.json         # Recommended extensions
│
└── docs/                       # Documentation
    ├── DOCKER_GUIDE.md         # Detailed Docker guide
    ├── POSTGRESQL_SETUP.md     # Database setup
    └── IMPLEMENTATION_README.md # Implementation details
```

---

## 📚 Documentation

- **[DOCKER_GUIDE.md](./DOCKER_GUIDE.md)** - Complete Docker documentation
- **[POSTGRESQL_SETUP.md](./POSTGRESQL_SETUP.md)** - Database setup alternatives
- **[IMPLEMENTATION_README.md](./IMPLEMENTATION_README.md)** - Implementation details
- **[CORRECTIONS.md](./CORRECTIONS.md)** - Bug fixes and updates

---

## 🎯 Development Workflows

### 1. Full Stack Docker (Recommended for beginners)

```bash
docker compose -f docker compose.dev.yml up
# Edit code → automatically reloaded!
```

### 2. Frontend only in Docker

```bash
docker compose up -d postgres backend
cd frontend && npm start
```

### 3. Backend only in Docker

```bash
docker compose up -d postgres frontend
cd backend && mvn spring-boot:run
```

### 4. Everything local (no Docker)

See [POSTGRESQL_SETUP.md](./POSTGRESQL_SETUP.md) for local PostgreSQL setup.

---

## 🌟 Features Implemented

✅ **UC001 - Manage Buildings** (Complete)
- Create building
- Edit building
- Delete building
- View buildings list (pagination, search, filter)
- Search by name/address

**User Stories**: US001, US002, US003, US004, US005

---

## 🚧 Roadmap

### Phase 2 - Housing Units
- UC002: Manage housing units
- Rooms management
- PEB scores tracking

### Phase 3 - Tenants & Leases
- Tenant management
- Lease contracts
- Payment tracking

See [docs/analysis/backlog.md](./docs/analysis/backlog.md) for full roadmap.

---

## 🤝 Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for development guidelines.

---

## 📄 License

Proprietary - ImmoCare Project

---

## 💡 Quick Tips

1. **Use make commands** for simplicity: `make up`, `make dev`, `make logs`
2. **Use VS Code tasks** for even more convenience (Ctrl+Shift+P → Run Task)
3. **Enable hot reload** with dev mode: `make dev`
4. **Debug in VS Code** by pressing F5 after starting dev mode
5. **Check health** regularly: `make health`

---

**Need help?** Check [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) for detailed documentation.

**Questions?** Open an issue on GitHub.

---

**Last Updated**: 2024-01-15  
**Version**: 2.0.0 - Full Docker Support  
**Status**: ✅ Production Ready
