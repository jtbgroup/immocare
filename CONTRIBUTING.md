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

### 1. Pick a User Story

- Check the Sprint board (GitHub Projects or Jira)
- Assign yourself to an unassigned user story
- Move card to "In Progress"

### 2. Create Feature Branch

```bash
git checkout main
git pull origin main
git checkout -b feature/US###-short-description
```

**Branch naming convention:**
- `feature/US001-create-building` - New features
- `bugfix/fix-validation-error` - Bug fixes
- `hotfix/critical-security-patch` - Urgent fixes
- `refactor/improve-service-layer` - Code improvements

### 3. Implement the Feature

Follow the acceptance criteria in the user story document.

### 4. Write Tests

- Unit tests (required)
- Integration tests (recommended)
- E2E tests (for critical flows)

### 5. Test Locally

```bash
# Backend tests
cd code/backend
mvn test

# Frontend tests
cd code/frontend
npm test

# Run application
docker-compose up -d postgres
mvn spring-boot:run  # Backend
npm start            # Frontend
```

### 6. Create Pull Request

See [Pull Request Process](#pull-request-process)

---

## Coding Standards

### Backend (Java)

#### Code Style

- **Formatter**: Google Java Format
- **Indentation**: 2 spaces
- **Line length**: Max 100 characters
- **Imports**: No wildcard imports

#### Naming Conventions

```java
// Classes: PascalCase
public class BuildingService { }

// Methods: camelCase
public Building createBuilding(BuildingDTO dto) { }

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

**2. Use DTOs for API:**
```java
// Controller
@PostMapping("/buildings")
public ResponseEntity<BuildingDTO> create(@Valid @RequestBody BuildingDTO dto) {
    Building building = buildingService.create(dto);
    return ResponseEntity.ok(mapper.toDTO(building));
}
```

**3. Validate Input:**
```java
public record BuildingDTO(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 200) String streetAddress,
    // ...
) { }
```

**4. Handle Exceptions:**
```java
@ControllerAdvice
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

- **Formatter**: Prettier
- **Linter**: ESLint
- **Indentation**: 2 spaces
- **Quotes**: Single quotes

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

**1. Use Reactive Forms:**
```typescript
buildingForm = this.fb.group({
  name: ['', [Validators.required, Validators.maxLength(100)]],
  streetAddress: ['', Validators.required],
  // ...
});
```

**2. Unsubscribe from Observables:**
```typescript
export class BuildingListComponent implements OnDestroy {
  private destroy$ = new Subject<void>();
  
  ngOnInit() {
    this.buildingService.getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe(buildings => this.buildings = buildings);
  }
  
  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

**3. Use Smart/Dumb Component Pattern:**
```typescript
// Smart Component (Container)
@Component({ selector: 'app-building-list-container' })
export class BuildingListContainerComponent {
  buildings$ = this.service.getAll();
  constructor(private service: BuildingService) {}
}

// Dumb Component (Presentational)
@Component({ selector: 'app-building-list' })
export class BuildingListComponent {
  @Input() buildings: Building[];
  @Output() buildingSelected = new EventEmitter<Building>();
}
```

---

### Database

#### Migration Naming

```
V001__create_users_table.sql
V002__create_buildings_table.sql
V003__add_owner_to_building.sql
```

- Always sequential version numbers
- Double underscore after version
- Descriptive name in snake_case

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

### Backend Tests

#### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {
    
    @Mock
    private BuildingRepository repository;
    
    @InjectMocks
    private BuildingService service;
    
    @Test
    void createBuilding_WithValidData_ReturnsSavedBuilding() {
        // Given
        BuildingDTO dto = new BuildingDTO("Test Building", ...);
        Building expected = new Building();
        when(repository.save(any())).thenReturn(expected);
        
        // When
        Building result = service.create(dto);
        
        // Then
        assertNotNull(result);
        verify(repository).save(any());
    }
}
```

#### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class BuildingControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void createBuilding_ReturnsCreated() throws Exception {
        String json = """
            {
                "name": "Test Building",
                "streetAddress": "123 Test St"
            }
        """;
        
        mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Building"));
    }
}
```

### Frontend Tests

#### Component Tests

```typescript
describe('BuildingListComponent', () => {
  let component: BuildingListComponent;
  let fixture: ComponentFixture<BuildingListComponent>;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [BuildingListComponent]
    });
    fixture = TestBed.createComponent(BuildingListComponent);
    component = fixture.componentInstance;
  });
  
  it('should display buildings', () => {
    component.buildings = [{ id: 1, name: 'Test' }];
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.building-name').textContent)
      .toContain('Test');
  });
});
```

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

Implements US001 - Create Building.
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
[US###] Short description
```

**Description template:**
```markdown
## User Story
Closes #123 (link to user story issue)

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
public Building create(BuildingDTO dto) {
    // Implementation
}
```

### API Documentation

Use Swagger/OpenAPI annotations:

```java
@Operation(summary = "Create a new building")
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Building created"),
    @ApiResponse(responseCode = "400", description = "Invalid input")
})
@PostMapping("/buildings")
public ResponseEntity<BuildingDTO> create(@RequestBody BuildingDTO dto) {
    // Implementation
}
```

### Update Analysis Docs

When implementing features, update:
- Mark user story as "Completed"
- Add technical notes if needed
- Update README if architecture changes

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
