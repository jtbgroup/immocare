# Contributing to ImmoCare

Thank you for your interest in contributing to ImmoCare! This document provides guidelines and best practices for development.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Development Workflow](#development-workflow)
3. [Coding Standards](#coding-standards)
4. [Testing Guidelines](#testing-guidelines)
5. [Git Workflow](#git-workflow)
6. [Pull Request Process](#pull-request-process)
7. [Documentation](#documentation)

---

## Getting Started

### Prerequisites

- Complete the [INSTALLATION.md](./INSTALLATION.md) setup
- Read the [docs/analysis/](./docs/analysis/) documentation
- Understand the project architecture

### Development Environment

- **Backend**: IntelliJ IDEA or VS Code with Java extensions
- **Frontend**: VS Code with Angular Language Service
- **Database**: PostgreSQL via Docker
- **API Testing**: Postman or Insomnia

---

## Development Workflow

### 1. Define the Change

- Clarify the business concepts, rules, and behavior affected by the change.
- Review the relevant business documentation and existing implementation.
- Track work in the project's issue system when applicable.

### 2. Create Feature Branch

```bash
git checkout main
git pull origin main
git checkout -b feature/short-description
```

**Branch naming convention:**
- `feature/add-building-capability` - New features
- `bugfix/fix-validation-error` - Bug fixes
- `hotfix/critical-security-patch` - Urgent fixes
- `refactor/improve-service-layer` - Code improvements

### 3. Implement the Change

Follow the relevant business rules and agreed behavior. Update business documentation when a change alters a business concept or rule.

### 4. Write Tests

- Unit tests (required)
- Integration tests (recommended)
- E2E tests (for critical flows)

### 5. Test Locally

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd ../frontend
npm test -- --watch=false
npm run test:e2e

# Run application
docker-compose up -d postgres
cd ../backend && mvn spring-boot:run
cd ../frontend && npm start
```

The Java tests currently live under `backend/src/test/com`, rather than Maven's conventional `backend/src/test/java`; verify that Maven Surefire reports tests were actually executed. The Angular project configures Jasmine/Karma and Playwright, but currently has no unit-test spec files. Add and run relevant tests for changed behavior.

### 6. Create Pull Request

See [Pull Request Process](#pull-request-process)

---

## Coding Standards

### Backend (Java)

#### Code Style

- No Java formatter or linter is currently configured in the Maven build. Follow nearby code formatting; do not claim Google Java Format is enforced.
- Follow the existing Java style and avoid wildcard imports.
- To enforce formatting, first add a pinned Maven formatter plugin, a check-only CI command, and a separately documented auto-fix command.

#### Naming Conventions

```java
// Classes: PascalCase
public class BuildingService { }

// Methods: camelCase
public BuildingDTOs.BuildingResponse createBuilding(
    BuildingDTOs.CreateBuildingRequest request) { }

// Constants: UPPER_SNAKE_CASE
private static final String DEFAULT_COUNTRY = "Belgium";

// Variables: camelCase
private String buildingName;
```

#### Package Structure

```
com.immocare
├── config          # Configuration classes
├── controller      # REST controllers
├── service         # Business logic
├── repository      # Data access
├── model           # Entities
│   ├── entity      # JPA entities
│   └── dto         # Data Transfer Objects
├── exception       # Custom exceptions
├── security        # Security configuration
└── util            # Utility classes
```

#### Best Practices

**1. Use Constructor Injection:**
```java
@Service
public class BuildingService {
    private final BuildingRepository repository;
    
    public BuildingService(BuildingRepository repository) {
        this.repository = repository;
    }
}
```

**2. Use DTO records grouped by domain:**
```java
// BuildingDTOs.java
public final class BuildingDTOs {
    private BuildingDTOs() {}

    public record CreateBuildingRequest(
        @NotBlank @Size(max = 100) String name
    ) {}

    public record BuildingResponse(Long id, String name) {}
}

// Map entities to DTOs in the service layer; never return entities from controllers.
@PostMapping("/buildings")
public ResponseEntity<BuildingDTOs.BuildingResponse> create(
        @Valid @RequestBody BuildingDTOs.CreateBuildingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(buildingService.create(request));
}
```

**3. Handle Exceptions:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDTO> handle(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorDTO(ex.getMessage()));
    }
}
```

---

### Frontend (Angular + TypeScript)

#### Code Style

- No frontend formatter or linter is currently configured by `package.json`. Follow nearby code formatting; do not assume Prettier or ESLint is installed or enforced.
- To enforce linting/formatting, configure Angular ESLint and Prettier, add `lint` and `format:check` npm scripts plus a separate auto-fix script, then run the checks in CI.
- **Indentation**: 2 spaces
- **Quotes**: Double quotes, matching the existing application code

#### Naming Conventions

```typescript
// Components: kebab-case files, PascalCase class
building-list.component.ts
export class BuildingListComponent { }

// Services: kebab-case file, PascalCase class with 'Service' suffix
building.service.ts
export class BuildingService { }

// Interfaces: PascalCase
export interface Building { }

// Constants: UPPER_SNAKE_CASE
export const API_BASE_URL = 'http://localhost:8080/api/v1';
```

#### Project Structure

```
src/
├── app/
│   ├── core/              # Singleton services
│   │   ├── auth/
│   │   ├── interceptors/
│   │   └── guards/
│   ├── shared/            # Shared components/modules
│   │   ├── components/
│   │   ├── directives/
│   │   └── pipes/
│   ├── features/          # Feature modules
│   │   ├── building/
│   │   ├── housing-unit/
│   │   └── room/
│   └── models/            # TypeScript interfaces
├── assets/
└── environments/
```

#### Best Practices

- Use standalone components and Signals for new component/application state. RxJS state patterns are legacy; do not expand them without an observable integration need.
- Prefer functional `inject()` in new code. Existing constructor injection can remain until intentionally refactored.
- Use Transloco for user-visible strings and update both English and French translation files.
- Use Angular's built-in template control flow (`@if`, `@for`, `@switch`) for new templates.

**Reactive forms:**
```typescript
buildingForm = this.fb.group({
  name: ['', [Validators.required, Validators.maxLength(100)]],
  streetAddress: ['', Validators.required],
  // ...
});
```

**Signal state:**
```typescript
export class BuildingListComponent {
  private readonly buildingService = inject(BuildingService);
  readonly buildings = signal<Building[]>([]);
  readonly isLoading = signal(false);
  readonly errorKey = signal<string | null>(null);

  load(): void {
    this.isLoading.set(true);
    this.errorKey.set(null);
    this.buildingService.getAll().subscribe({
      next: (buildings) => this.buildings.set(buildings),
      error: () => {
        this.errorKey.set('building.loadFailed');
        this.isLoading.set(false);
      },
      complete: () => this.isLoading.set(false),
    });
  }
}
```

Keep HTTP services typed and observable-based where appropriate, but make Signals the state model in new UI code. Surface failures through established error and localization patterns; do not silently swallow them.

---

### Database

#### Migration Naming

```
V018__add_building_external_reference.sql
V019__enforce_equipment_owner.sql
```

- Always sequential version numbers
- Double underscore after version
- Descriptive business/domain name in snake_case; do not use use-case or story identifiers
- Migrations are forward-only. Preserve existing data and explicitly handle legacy rows when changing constraints or ownership

#### SQL Style

```sql
-- Use uppercase for keywords
CREATE TABLE building (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for foreign keys
CREATE INDEX idx_building_created_by ON building(created_by);

-- Add comments
COMMENT ON TABLE building IS 'Physical buildings containing housing units';
```

---

## Testing Guidelines

### Backend

The project uses JUnit 5 and Spring Boot Test. Use MockMvc for HTTP behavior and estate authorization/isolation tests; use Mockito and AssertJ when appropriate.

```bash
cd backend
mvn test
```

Existing tests are currently under `backend/src/test/com`, rather than Maven's conventional `backend/src/test/java`. Verify Surefire reports a non-zero executed-test count. If tests are not discovered, move them to the conventional test source root or explicitly configure Maven and validate that CI executes them.

### Frontend

The configured unit test runner is Angular CLI with Jasmine/Karma. No `*.spec.ts` unit-test files currently exist. Add focused specs alongside changed UI code and verify the command reports executed tests:

```bash
cd frontend
npm test -- --watch=false
npm run test:e2e
```

Playwright E2E tests are under `frontend/e2e/`. Cover critical user journeys and verify API-backed behavior, not only static rendering.

### Tooling gaps to resolve before enforcing style checks

- There are no configured formatter or lint commands in either project manifest. Do not describe Google Java Format, Prettier, or ESLint as active requirements until the tools are installed and configured.
- For Java formatting, add a pinned Maven formatter plugin with separate check and apply goals, and run the check goal in CI.
- For Angular linting and formatting, configure Angular ESLint and Prettier, add `lint`, `format:check`, and a separate auto-fix script to `package.json`, and run the check scripts in CI.
- Update these instructions and the root contributor documentation with the exact verified commands only after the configuration is in place.

---

## Git Workflow

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, no logic change)
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(building): add create building endpoint

- Add BuildingController with POST /api/v1/buildings
- Add BuildingService.create() method
- Add validation for required fields

Closes #123
```

```
fix(auth): resolve login token expiration issue

Token was expiring too quickly due to incorrect time unit.
Changed from seconds to milliseconds.

Fixes #456
```

### Branch Strategy

- **main**: Production-ready code
- **develop**: Integration branch (optional)
- **feature/**: Feature development
- **bugfix/**: Bug fixes
- **hotfix/**: Urgent production fixes

---

## Pull Request Process

### 1. Before Creating PR

- [ ] All tests pass
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] No merge conflicts with main
- [ ] Commit messages follow convention

### 2. Create PR

**Title format:**
```
<type>(<scope>): <short description>
```

**Description template:**
```markdown
## Business behavior
Describe the business concept or rule affected and the expected behavior.

## Changes
- Added BuildingController
- Implemented create building endpoint
- Added validation

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manually tested in local environment

## Screenshots (if applicable)
[Add screenshots]

## Checklist
- [ ] Code reviewed by myself
- [ ] Tests pass
- [ ] Documentation updated
- [ ] No breaking changes
```

### 3. Code Review

- At least 1 approval required
- Address all comments
- Keep discussion professional and constructive

### 4. Merge

- Squash and merge (preferred)
- Delete branch after merge

---

## Documentation

### Code Comments

```java
/**
 * Creates a new building in the system.
 *
 * @param dto the building data transfer object
 * @return the created building entity
 * @throws ValidationException if dto validation fails
 */
public BuildingDTOs.BuildingResponse create(
        BuildingDTOs.CreateBuildingRequest request) {
    // Implementation
}
```

### API Documentation

The project does not currently configure OpenAPI generation or an API documentation library. Keep API contracts synchronized in backend DTOs/routes, frontend models/services, and relevant tests. Do not add Swagger/OpenAPI annotations as a presumed existing convention; introduce the dependency and workflow deliberately if API generation is adopted.

### Update Analysis Docs

When business behavior or domain meaning changes, update the relevant business concept/rule documentation. Update technical documentation when API contracts, data structures, or architecture change.

---

## Code Review Checklist

### For Author

- [ ] Code follows style guidelines
- [ ] All tests pass
- [ ] No console.log or debug statements
- [ ] No commented-out code
- [ ] Error handling implemented
- [ ] Security considerations addressed
- [ ] Performance implications considered

### For Reviewer

- [ ] Logic is correct
- [ ] Code is readable and maintainable
- [ ] Tests are comprehensive
- [ ] Edge cases handled
- [ ] Documentation adequate
- [ ] No security vulnerabilities
- [ ] Follows project patterns

---

## Release Process

### Versioning

Use [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

### Release Steps

1. Update version in `pom.xml` and `package.json`
2. Update CHANGELOG.md
3. Create release branch: `release/v0.1.0`
4. Test thoroughly
5. Merge to main
6. Tag: `git tag -a v0.1.0 -m "Release version 0.1.0"`
7. Push tags: `git push origin --tags`
8. Create GitHub release with notes

---

## Getting Help

- **Questions**: Ask in team chat
- **Bug reports**: Create GitHub issue
- **Feature requests**: Discuss with team first
- **Documentation**: Check docs/ folder

---

## License

This project is proprietary. See LICENSE file.

---

**Last Updated**: 2024-01-15  
**Version**: 1.0
