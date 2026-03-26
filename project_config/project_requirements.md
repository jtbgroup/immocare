This is the baseline file for the creation, context, structure , ... of the project. This file can be used to generate the structure from scratch or complete / modify an existing project repository.

The project will be a web based application in monorepo, meaning the backend and the frontend will be served as a global package, presenting only one port to the user. Routing (front - back) will be done by the application itself. The application will ebe server as a container.

# Stack

- Backend 
  - Java 21+
  - Spring Boot 3+
  - Spring Security (session-based auth, BCrypt 12)
  - Spring Data JPA
  - Flyway
  - PostgreSQL 17+
- Frontend: 
  - Angular 19+
  - Angular Material
  - standalone components
  - lazy-loaded feature modules
- Infrastructure
  - Docker Compose with two separate environments
    - *docker-compose.dev.yml*: development with hot reload (frontend on port 4300, backend on port 8080)
    - *docker-compose.yml*: production with multi-stage Dockerfile (app on port 8090)

the code generation for this stack must follow the best coding practice and must never use deprecated or legacy way of coding.

# Structure of the project

The project will be organized in a standard way to easily find all the needed resources. 

at the root level, you'll find:

- folder **fontend** containing all the code for the frontend
- folder **backend** containing all the code for the frontend
- folder **.github** containing the github actions
- folder **.git** containing the git files
- folder **docker** containing the docker specific files
- folder **doc** containing the docker specific files
- file **.gitignore** defining the ignored files
- file **Dockerfile** for building the docker
- file **MakeFile** shortcut commands for daily usage
 
## fontend

Contains not only the code of the frontend but also the frontend automated tests located in a **e2e** folder.
Don't use *ngIf and *ngFor syntax; use built-in @if / @for instead.

## backend

Contains the code of the backend including the Flyway files. 

Each Flyway migration is traceable to a use case (SQL header comment: -- Use case: UC-XX). There must be maximum one Flyway file per use case. This must remain synchronized with the documentation (folder **doc/use_cases**) 

## doc

All the documentation must be in markdown format. Documentation is separated in 2 families: 

### analysis

**use cases** 

Located in the **use_cases** folder. Each use case lives in its own file: `doc/analysis/use-cases/UC-XX-<slug>.md`
    
**user stories**

Located in the **user_stories** folder. Each user story lives in its own file: `doc/analysis/user-stories/US-XXX-<slug>.md`

**prompts**

Located in the **prompts** folder. Each UC has a dedicated generation prompt: `doc/analysis/prompts/UC-XX-<slug>-prompt.md`. The content of the prompt must allow the generation of the code for the specific use case. This includes the frontend, the backend, the flyway files, the resources and any other material required.

### architecture

Located in the **architecture** folder. It will contain all the technical documentation of the project like SAD (software architecture documents), data base schemas, data model, api definitions, ...

The minimum required files are:
- sad.md: showing all the application component
- data-model.md : describing the backend datamodel
- api.md: assemble the api of the backend defined in the other documentation files. This must be a potential base for a swagger for example.

### other

Beside those folders, a complementary file is needed to define the roles and access matrix: `doc/roles.md`. The file is at the root level of the **doc** folder

# Authentication:

The minimum requirement is a session-based DB auth but it must be possible to add other authentication methods like OAuth2 through a Keycloak.

The Spring Security config MUST be structured so switching to OAuth2/Keycloak (or any other authentication provider) only requires adding the resource server starter and swapping the SecurityFilterChain bean — no other changes.