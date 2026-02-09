# Technical Requirements

## Project Architecture

### Overview

SlaGen is built as a **mono-repository** containing both frontend and backend code, along with complete documentation, configuration files, and deployment scripts. This approach facilitates:

- Coordinated development across frontend and backend
- Unified version control
- Simplified dependency management
- Single source of truth for all project artifacts

### Repository Structure
```
slaGen/
├── backend/              # Spring Boot application
├── frontend/             # Angular application
├── docker/               # Container orchestration
├── docs/                 # Complete documentation
├── README.md
├── INSTALLATION.md
├── CONTRIBUTING.md
└── ...
```

## Technology Stack

### Backend

#### Core Framework
- **Java**: Version 17 (LTS)
  - Modern Java features (records, switch expressions, text blocks)
  - Strong typing and compile-time safety
  - Excellent tooling and ecosystem

- **Spring Boot**: Version 3.x
  - Rapid application development
  - Production-ready features (actuator, metrics)
  - Extensive ecosystem and community support

#### Key Dependencies

- **Spring Security**
  - Authentication and authorization
  - Role-based access control (RBAC)
  - Password encryption (BCrypt)
  - Session management
  - CSRF protection

- **Hibernate / JPA**
  - Object-Relational Mapping (ORM)
  - Database abstraction
  - Query optimization
  - Entity relationship management

- **Flyway**
  - Database version control
  - Automated migrations
  - Repeatable scripts
  - Schema evolution tracking

- **Maven**
  - Dependency management
  - Build automation
  - Multi-module support
  - Plugin ecosystem

#### Additional Libraries (Planned/Suggested)

- **iText / Apache PDFBox**: PDF generation
- **MapStruct**: DTO mapping
- **Lombok**: Reduce boilerplate code
- **Spring Validation**: Input validation
- **Spring Boot Actuator**: Monitoring and health checks
- **JUnit 5**: Unit testing
- **Mockito**: Mocking framework
- **TestContainers**: Integration testing with containers

### Frontend

#### Core Framework
- **Angular**: Version 17+
  - Component-based architecture
  - TypeScript for type safety
  - Reactive programming with RxJS
  - Powerful CLI for development
  - Strong community and enterprise adoption

- **TypeScript**: Latest stable version
  - Static typing
  - Enhanced IDE support
  - Improved code maintainability
  - Better refactoring capabilities

#### Key Dependencies (Suggested)

- **Angular Material** or **PrimeNG**: UI component library
- **RxJS**: Reactive extensions for async operations
- **Angular Forms**: Reactive forms for complex inputs
- **Angular Router**: Navigation and routing
- **HttpClient**: REST API communication

#### Testing

- **Jasmine/Karma**: Unit testing
- **Cypress**: End-to-end testing
  - User workflow testing
  - Integration testing
  - Visual regression testing

**Note**: The Angular frontend is one possible client of the API. The backend API is designed to be consumed by any HTTP client, not just Angular. Future implementations could include React, Vue, mobile apps, or other frontends using the exact same API.

### Database

#### PostgreSQL

- **Version**: 15+
- **Rationale**:
  - ACID compliance for data integrity
  - Advanced features (JSON support, full-text search)
  - Excellent performance and scalability
  - Strong community and enterprise support
  - Open source with permissive license

#### Database Strategy

- **Schema Management**: Flyway migrations
- **Connection Pooling**: HikariCP (default in Spring Boot)
- **Data Access**: JPA/Hibernate
- **Indexes**: Strategic indexing for performance
- **Constraints**: Enforce data integrity at DB level

### Infrastructure

#### Containerization

- **Docker**
  - Isolated environments
  - Consistent deployments
  - Easy scaling
  - Platform independence

- **Docker Compose**
  - Multi-container orchestration
  - Development environment setup
  - Service dependencies management
  - Environment-specific configurations

#### Reverse Proxy

- **Nginx**
  - Load balancing
  - SSL/TLS termination
  - Static file serving
  - Request routing (frontend/backend)
  - Compression and caching

#### Container Architecture
```
┌─────────────────────────────────────────┐
│            Nginx Container              │
│         (Reverse Proxy)                 │
│         Port: 80/443                    │
└──────────┬────────────────┬─────────────┘
           │                │
           │                │
    ┌──────▼──────┐  ┌─────▼──────────┐
    │  Frontend   │  │    Backend     │
    │  Container  │  │   Container    │
    │  (Angular)  │  │  (Spring Boot) │
    │  Port: 4200 │  │   Port: 8080   │
    └─────────────┘  └────────┬───────┘
                              │
                     ┌────────▼──────────┐
                     │   PostgreSQL      │
                     │    Container      │
                     │   Port: 5432      │
                     └───────────────────┘
```

## API-First Architecture

### Design Principle

SlaGen follows an **API-First design approach** where the REST API is the primary interface to all business logic. The API is completely independent of any frontend implementation.

### API Independence

**Key Characteristics**:
- **Frontend Agnostic**: API has no knowledge of which client consumes it
- **Reusable**: Same API can serve multiple frontends simultaneously
- **Versioned**: API evolution doesn't break existing clients
- **Standard-Based**: Uses standard HTTP methods, JSON format, RESTful principles

**Supported Clients**:
- ✅ Angular web application (current implementation)
- ✅ React/Vue web applications (future)
- ✅ Mobile applications (iOS, Android) (future)
- ✅ Command-line tools (future)
- ✅ Third-party integrations (future)
- ✅ Automated scripts and bots

### Communication Flow

┌─────────────────┐ │ Any Frontend │ (Angular, React, Mobile, CLI, etc.) │ Application │ └────────┬────────┘ │ HTTP/HTTPS │ JSON ▼ ┌─────────────────┐ │ REST API │ /api/v1/* │ (Backend) │ - Authentication │ │ - Business Logic │ │ - Data Access └────────┬────────┘ │ JDBC ▼ ┌─────────────────┐ │ PostgreSQL │ │ Database │ └─────────────────┘


### Benefits

1. **Flexibility**: Switch or add frontends without backend changes
2. **Parallel Development**: Frontend and backend teams work independently
3. **Testing**: API can be tested independently of UI
4. **Integration**: Easy integration with other systems
5. **Future-Proof**: New clients can be added without API changes
6. 
## Authentication & Authorization

### Phase 1: Embedded User Management

- **Storage**: PostgreSQL database
- **Password Encryption**: BCrypt
- **Session Management**: Spring Security
- **User Roles**: Editor, Validator (Smals), Validator (Defense), PMO
- **Access Control**: Method-level security with `@PreAuthorize`

### Phase 2: OAuth 2.0 / Keycloak (Roadmap)

- **Protocol**: OAuth 2.0 / OpenID Connect
- **Identity Provider**: Keycloak
- **Benefits**:
  - Centralized authentication
  - Single Sign-On (SSO)
  - Multi-factor authentication (MFA)
  - Integration with enterprise directory (LDAP/AD)

## Development Environment

### Required Tools

- **JDK 17**: Backend development
- **Node.js 18+**: Frontend development
- **Maven 3.8+**: Build automation
- **Docker Desktop**: Container development
- **Git**: Version control
- **IDE**: IntelliJ IDEA (recommended) or VS Code

### Development Setup

- **Backend**: Runs on `http://localhost:8080`
- **Frontend**: Runs on `http://localhost:4200`
- **Database**: Runs on `localhost:5432`
- **Hot Reload**: Enabled for both frontend and backend

### Environment Profiles

- **dev**: Development with debug enabled, H2/local PostgreSQL
- **test**: Testing with in-memory database or TestContainers
- **prod**: Production with optimizations and security hardening

## Deployment Strategy

### Development Deployment
```bash
docker-compose -f docker-compose.dev.yml up
```

- Hot reload enabled
- Debug logging
- Development databases
- No SSL

### Production Deployment
```bash
docker-compose -f docker-compose.prod.yml up -d
```

- Optimized builds
- Production logging
- Persistent volumes
- SSL/TLS enabled
- Resource limits configured

### Build Process

1. **Backend Build**:
```bash
   mvn clean package -DskipTests
```
   Produces: `target/slagen-0.1.0.jar`

2. **Frontend Build**:
```bash
   npm run build --prod
```
   Produces: `dist/` folder

3. **Docker Image Build**:
```bash
   docker build -t slagen-backend:0.1.0 .
   docker build -t slagen-frontend:0.1.0 .
```

## Non-Functional Requirements

### Performance

- **Page Load Time**: < 2 seconds
- **API Response Time**: < 500ms (95th percentile)
- **Concurrent Users**: Support 50+ simultaneous users
- **Database Queries**: Optimized with proper indexes
- **PDF Generation**: < 3 seconds per document

### Scalability

- **Horizontal Scaling**: Backend can be scaled with multiple containers
- **Database**: Connection pooling to handle concurrent requests
- **Caching**: Strategic caching for frequently accessed data (future)

### Security

#### Authentication
- Secure password storage (BCrypt with salt)
- Session timeout after inactivity
- Account lockout after failed login attempts

#### Authorization
- Role-based access control (RBAC)
- Method-level security
- URL-based security rules

#### Data Protection
- SQL injection prevention (JPA/Hibernate)
- XSS protection
- CSRF tokens
- Input validation and sanitization
- Secure headers (HSTS, X-Frame-Options, etc.)

#### Transport Security
- HTTPS in production
- TLS 1.2+ only
- Strong cipher suites

### Reliability

- **Uptime Target**: 99% availability
- **Data Backup**: Daily automated backups
- **Recovery Time**: < 4 hours
- **Error Handling**: Graceful degradation, user-friendly error messages

### Maintainability

- **Code Coverage**: > 80% for critical business logic
- **Documentation**: All public APIs documented
- **Logging**: Structured logging with appropriate levels
- **Monitoring**: Health checks and metrics endpoints

### Usability

- **Browser Support**:
  - Chrome (latest)
  - Firefox (latest)
  - Edge (latest)
  - Safari (latest)
- **Responsive Design**: Desktop and tablet support
- **Accessibility**: WCAG 2.1 Level AA compliance (goal)

## Data Management

### Database Schema

- **Normalization**: 3NF where appropriate
- **Indexes**: On foreign keys and frequently queried columns
- **Constraints**: Primary keys, foreign keys, unique constraints
- **Audit Fields**: created_at, updated_at, created_by, updated_by

### Data Retention

- **Active SLAs**: Indefinite retention
- **Archived SLAs**: Retained for compliance (minimum 7 years)
- **Audit Logs**: Retained for 3 years
- **User Sessions**: 24 hours or until logout

### Backup Strategy

- **Frequency**: Daily automated backups
- **Retention**: 30 days rolling
- **Storage**: Separate backup location
- **Testing**: Monthly restore tests

## API Design

### RESTful Principles

- **Resources**: Nouns representing entities (e.g., `/api/slas`, `/api/systems`)
- **HTTP Methods**: GET (read), POST (create), PUT (update), DELETE (delete)
- **Status Codes**: Appropriate HTTP status codes
- **Versioning**: `/api/v1/...` for future compatibility

### Request/Response Format

- **Content-Type**: `application/json`
- **Date Format**: ISO 8601 (e.g., `2024-01-15T10:30:00Z`)
- **Error Format**: Standardized error response structure

### Example Endpoints
```
GET    /api/v1/slas              # List all SLAs
GET    /api/v1/slas/{id}         # Get SLA by ID
POST   /api/v1/slas              # Create new SLA
PUT    /api/v1/slas/{id}         # Update SLA
DELETE /api/v1/slas/{id}         # Delete SLA
POST   /api/v1/slas/{id}/submit  # Submit for validation
POST   /api/v1/slas/{id}/validate # Validate SLA
GET    /api/v1/slas/{id}/pdf     # Generate PDF
```

## Version Control Strategy

### Git Workflow

- **Main Branch**: `main` - Production-ready code
- **Feature Branches**: `feature/description` - New features
- **Bugfix Branches**: `bugfix/description` - Bug fixes
- **Hotfix Branches**: `hotfix/description` - Urgent production fixes

### Commit Messages

- Follow [Conventional Commits](https://www.conventionalcommits.org/)
- Format: `type(scope): subject`
- Types: feat, fix, docs, style, refactor, test, chore

### Release Strategy

- **Semantic Versioning**: MAJOR.MINOR.PATCH
- **Tags**: Each release tagged (e.g., `v0.1.0`)
- **Release Notes**: Updated with each version

## Testing Strategy

### Unit Tests
- **Backend**: JUnit 5, Mockito
- **Frontend**: Jasmine, Karma
- **Coverage**: > 80% for business logic

### Integration Tests
- **Backend**: Spring Boot Test, TestContainers
- **Database**: Test with real PostgreSQL container

### End-to-End Tests
- **Tool**: Cypress
- **Scope**: Critical user workflows
- **Frequency**: Before each release

### Test Environments
- **Local**: Developer machines
- **CI**: Automated testing on commit/PR
- **Staging**: Pre-production environment (future)

## Monitoring & Logging

### Application Monitoring

- **Spring Boot Actuator**: Health checks, metrics
- **Endpoints**:
  - `/actuator/health` - Application health
  - `/actuator/info` - Application info
  - `/actuator/metrics` - Application metrics

### Logging

- **Framework**: SLF4J with Logback
- **Levels**: ERROR, WARN, INFO, DEBUG
- **Format**: Structured JSON logs
- **Rotation**: Daily rotation, 30 days retention

### Log Categories

- **Application**: Business logic events
- **Security**: Authentication/authorization events
- **Performance**: Slow queries, long requests
- **Error**: Exceptions and failures

## Configuration Management

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=slagen
DB_USER=slagen_user
DB_PASSWORD=secure_password

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
JWT_SECRET=your_secret_key

# Frontend
API_URL=http://localhost:8080/api
```

### Configuration Files

- `application.yml` - Default configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides

### Secrets Management

- **Development**: `.env` files (not committed)
- **Production**: Environment variables or secret management system
- **Never commit**: Passwords, API keys, certificates

## Development Workflow

### Local Development

1. Start database: `docker-compose up postgres`
2. Run backend: `mvn spring-boot:run`
3. Run frontend: `npm start`
4. Access application: `http://localhost:4200`

### Code Quality

- **Linting**: ESLint (frontend), Checkstyle (backend)
- **Formatting**: Prettier (frontend), Google Java Format (backend)
- **Code Review**: Required for all changes
- **Static Analysis**: SonarQube (future)

## Constraints & Limitations

### Technical Constraints

- Must use Java 17+ (LTS support)
- Must use PostgreSQL (no other databases)
- Must be containerized
- Must support Docker Compose deployment

### Development Constraints

- Mono-repo structure (no separate repositories)
- All documentation in Markdown
- English for all code and documentation

### Deployment Constraints

- Must run on Linux containers
- Must support offline installation (air-gapped)
- Resource limits: 2 CPU cores, 4GB RAM per container

## Future Considerations

### Scalability Enhancements

- Load balancing across multiple backend instances
- Database read replicas
- Caching layer (Redis)
- CDN for static assets

### Integration Possibilities

- CMDB integration
- Ticketing system integration
- Monitoring tool integration
- API gateway

### Advanced Features

- GraphQL API
- WebSocket for real-time updates
- Elasticsearch for advanced search
- Kubernetes deployment

---

**Last Updated**: 2024-01-15
**Maintained By**: Development Team