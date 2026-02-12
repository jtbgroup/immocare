# ImmoCare

**Property Management System for Buildings, Housing Units, and Rental Operations**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17-red.svg)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey.svg)](LICENSE)

---

## 📋 Overview

ImmoCare is a comprehensive property management system designed to manage:

- 🏢 **Buildings**: Physical properties with addresses and ownership
- 🏠 **Housing Units**: Individual apartments/units within buildings
- 📐 **Rooms**: Room composition with types and surfaces
- ⚡ **Energy Performance**: PEB (Energy Performance Certificate) tracking
- 💰 **Rents**: Indicative rent amounts with historical tracking
- 💧 **Utilities**: Water meter assignments (electricity, gas in future phases)

---

## 🚀 Quick Start

### Prerequisites

- Java 17 (LTS)
- Node.js 18+
- Maven 3.8+
- Docker Desktop
- PostgreSQL 15+ (via Docker)

### Installation

```bash
# Clone the repository
git clone https://github.com/your-org/immocare.git
cd immocare

# Start PostgreSQL
docker-compose up -d postgres

# Backend setup
cd code/backend
mvn clean install
mvn flyway:migrate
mvn spring-boot:run

# Frontend setup (in new terminal)
cd code/frontend
npm install
npm start
```

**Access the application:**
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080
- API Documentation: http://localhost:8080/swagger-ui.html

**Default credentials:**
- Username: `admin`
- Password: `Admin123!`

> ⚠️ **Important**: Change the default password immediately in production!

---

## 📁 Project Structure

```
immocare/
├── code/
│   ├── backend/              # Spring Boot API
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/
│   │   │   │   └── resources/
│   │   │   │       └── db/migration/  # Flyway SQL scripts
│   │   │   └── test/
│   │   └── pom.xml
│   └── frontend/             # Angular application
│       ├── src/
│       │   ├── app/
│       │   ├── assets/
│       │   └── environments/
│       └── package.json
├── docs/
│   └── analysis/             # Complete project documentation
│       ├── data-model.md
│       ├── data-dictionary.md
│       ├── use-cases/        # 6 detailed use cases
│       ├── user-stories/     # 30 user stories with acceptance criteria
│       ├── roles-permissions.md
│       └── backlog.md
├── docker-compose.yml
├── INSTALLATION.md           # Detailed setup guide
├── CONTRIBUTING.md           # Development guidelines
└── README.md                 # This file
```

---

## 🛠️ Technology Stack

### Backend
- **Java 17** (LTS)
- **Spring Boot 3.2** (Web, Security, Data JPA)
- **PostgreSQL 15** (Database)
- **Flyway** (Database migrations)
- **JWT** (Authentication)
- **MapStruct** (DTO mapping)
- **Lombok** (Code reduction)
- **SpringDoc OpenAPI** (API documentation)

### Frontend
- **Angular 17** (Framework)
- **TypeScript** (Language)
- **RxJS** (Reactive programming)
- **Angular Material** / **PrimeNG** (UI components)

### Infrastructure
- **Docker** & **Docker Compose** (Containerization)
- **Nginx** (Reverse proxy - production)
- **Maven** (Backend build)
- **npm** (Frontend build)

---

## 🎯 Features

### Phase 1 (Current - Foundation)

- ✅ User authentication (ADMIN role)
- ✅ Building CRUD operations
- ✅ Housing Unit management
- ✅ Room composition
- ✅ PEB score tracking with history
- ✅ Rent tracking with history
- ✅ Water meter assignments

### Phase 2 (Planned)

- 🔄 Tenant management
- 🔄 Lease contracts
- 🔄 Payment tracking
- 🔄 Additional utility meters (electricity, gas, heating)
- 🔄 Role expansion (Property Manager, Accountant)

### Phase 3+ (Future)

- 📅 Maintenance management
- 📄 Document management
- 📊 Financial reporting & dashboards
- 🔔 Email notifications
- 👤 Tenant self-service portal

See [docs/analysis/backlog.md](docs/analysis/backlog.md) for complete roadmap.

---

## 📚 Documentation

### For Developers

- **[INSTALLATION.md](INSTALLATION.md)** - Complete setup guide
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development guidelines and standards
- **[docs/analysis/](docs/analysis/)** - Complete project analysis

### Key Documents

- **[Data Model](docs/analysis/data-model.md)** - Database schema and entity relationships
- **[Use Cases](docs/analysis/use-cases/)** - Detailed use case descriptions (UC001-UC006)
- **[User Stories](docs/analysis/user-stories/)** - 30 user stories with acceptance criteria
- **[API Documentation](http://localhost:8080/swagger-ui.html)** - Interactive API docs (when running)

---

## 🧪 Testing

### Backend Tests

```bash
cd code/backend

# Unit tests
mvn test

# Integration tests
mvn verify

# Code coverage
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

### Frontend Tests

```bash
cd code/frontend

# Unit tests
npm test

# Code coverage
npm run test:coverage

# E2E tests
npm run e2e
```

---

## 🏗️ Development

### Run Locally (Development Mode)

```bash
# Terminal 1 - Database
docker-compose up postgres

# Terminal 2 - Backend
cd code/backend
mvn spring-boot:run

# Terminal 3 - Frontend
cd code/frontend
npm start
```

### Code Style

- **Backend**: Google Java Format
- **Frontend**: Prettier + ESLint

```bash
# Format frontend code
cd code/frontend
npm run format
```

### Database Migrations

```bash
cd code/backend

# Create new migration
# Manually create: src/main/resources/db/migration/V009__description.sql

# Apply migrations
mvn flyway:migrate

# Clean database (⚠️ DESTRUCTIVE)
mvn flyway:clean
```

---

## 🚢 Deployment

### Build for Production

```bash
# Backend
cd code/backend
mvn clean package -DskipTests
# Output: target/immocare-backend-0.1.0.jar

# Frontend
cd code/frontend
npm run build:prod
# Output: dist/immocare-frontend/
```

### Docker Deployment

```bash
# Build images
docker-compose build

# Run all services
docker-compose --profile full-stack up -d

# Stop all services
docker-compose down
```

---

## 🔐 Security

### Authentication

- Username/password authentication
- BCrypt password hashing
- JWT token-based sessions
- Session timeout: 30 minutes

### Authorization

- Role-Based Access Control (RBAC)
- Current roles: ADMIN
- Method-level security with `@PreAuthorize`

### Security Features

- SQL injection prevention (JPA/Hibernate)
- XSS protection
- CSRF tokens
- Input validation
- Secure headers (HSTS, X-Frame-Options)

---

## 📊 Database Schema

The database consists of 7 main tables:

1. **app_user** - Authentication and users
2. **building** - Physical buildings
3. **housing_unit** - Individual apartments
4. **room** - Rooms within units
5. **peb_score_history** - Energy performance tracking
6. **rent_history** - Rent tracking over time
7. **water_meter_history** - Utility meter assignments

See [docs/analysis/data-model.md](docs/analysis/data-model.md) for complete schema.

---

## 🤝 Contributing

We welcome contributions! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for:

- Development workflow
- Coding standards
- Git workflow
- Pull request process
- Testing guidelines

### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(building): add create building endpoint
fix(auth): resolve token expiration issue
docs(readme): update installation instructions
```

---

## 📝 License

This project is proprietary and confidential. Unauthorized copying or distribution is prohibited.

---

## 👥 Team

- **Project Lead**: [Name]
- **Backend Developer**: [Name]
- **Frontend Developer**: [Name]
- **DevOps**: [Name]

---

## 📞 Support

- **Documentation**: [docs/](docs/)
- **Issues**: GitHub Issues
- **Email**: support@immocare.local
- **Wiki**: Project Wiki

---

## 🗺️ Roadmap

### v0.1.0 (Current - Q1 2024)
- ✅ Core CRUD operations
- ✅ Basic authentication
- ✅ Database schema

### v0.2.0 (Q2 2024)
- 🔄 Tenant management
- 🔄 Lease contracts
- 🔄 Payment tracking

### v0.3.0 (Q3 2024)
- 📅 Maintenance management
- 📄 Document management
- 📊 Basic reporting

See [docs/analysis/backlog.md](docs/analysis/backlog.md) for detailed roadmap.

---

## 🙏 Acknowledgments

- Spring Boot team
- Angular team
- PostgreSQL community
- All contributors

---

**Made with ❤️ by the ImmoCare Team**

---

**Last Updated**: 2024-01-15  
**Version**: 0.1.0
