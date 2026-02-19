Je veux implémenter le Use Case UC001 - Manage Buildings pour ImmoCare.

CONTEXTE:
- Projet: ImmoCare (property management system)
- Stack: Spring Boot 3.2 (backend) + Angular 17 (frontend) + PostgreSQL 15
- Architecture: API-First, mono-repo
- Database: Schéma déjà créé avec Flyway migrations

USER STORIES À IMPLÉMENTER:
1. US001 - Create Building (Priority: MUST HAVE, 5 points)
2. US002 - Edit Building (Priority: MUST HAVE, 3 points)
3. US003 - Delete Building (Priority: MUST HAVE, 3 points)
4. US004 - View Buildings List (Priority: MUST HAVE, 3 points)
5. US005 - Search Buildings (Priority: SHOULD HAVE, 2 points)

DOCUMENTS DE RÉFÉRENCE:
- docs/analysis/data-model.md : Entité Building avec tous les attributs
- docs/analysis/data-dictionary.md : Contraintes et validations
- docs/analysis/use-cases/UC001-manage-buildings.md : Flows détaillés
- docs/analysis/user-stories/US001-005 : Critères d'acceptation

STRUCTURE PROJET:
backend/
├── src/main/java/com/immocare/
│   ├── config/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   │   ├── entity/
│   │   └── dto/
│   ├── mapper/
│   ├── exception/
│   └── security/
├── src/main/resources/
│   └── db/migration/  (migrations déjà créées)
└── pom.xml

frontend/
├── src/app/
│   ├── core/
│   ├── shared/
│   ├── features/
│   │   └── building/
│   └── models/
└── package.json

BACKEND À CRÉER (dans backend/src/main/java/com/immocare/):
1. model/entity/Building.java
2. repository/BuildingRepository.java
3. service/BuildingService.java
4. controller/BuildingController.java
5. model/dto/BuildingDTO.java
6. model/dto/CreateBuildingRequest.java
7. model/dto/UpdateBuildingRequest.java
8. mapper/BuildingMapper.java (MapStruct)
9. exception/BuildingNotFoundException.java
10. Tests (dans src/test/):
    - service/BuildingServiceTest.java
    - controller/BuildingControllerTest.java

FRONTEND À CRÉER (dans frontend/src/app/):
1. models/building.model.ts
2. core/services/building.service.ts
3. features/building/
   ├── building.module.ts
   ├── building-routing.module.ts
   ├── components/
   │   ├── building-list/
   │   │   ├── building-list.component.ts
   │   │   ├── building-list.component.html
   │   │   └── building-list.component.scss
   │   ├── building-form/
   │   │   ├── building-form.component.ts
   │   │   ├── building-form.component.html
   │   │   └── building-form.component.scss
   │   └── building-details/
   │       ├── building-details.component.ts
   │       ├── building-details.component.html
   │       └── building-details.component.scss

API ENDPOINTS À IMPLÉMENTER:
- GET    /api/v1/buildings?page=0&size=20&sort=name,asc&city=Brussels&search=text
- GET    /api/v1/buildings/{id}
- POST   /api/v1/buildings
- PUT    /api/v1/buildings/{id}
- DELETE /api/v1/buildings/{id}

ENTITY BUILDING (référence):
- id: Long (PK, auto-generated)
- name: String (max 100, required)
- streetAddress: String (max 200, required)
- postalCode: String (max 20, required)
- city: String (max 100, required)
- country: String (max 100, required, default "Belgium")
- ownerName: String (max 200, optional)
- createdBy: User (FK, set null on delete)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

BUSINESS RULES (docs/analysis/data-model.md):
- BR-UC001-01: All required fields must be present
- BR-UC001-02: Owner inheritance to housing units
- BR-UC001-03: Cannot delete building with housing units
- BR-UC001-04: Duplicate building names allowed
- BR-UC001-05: No postal code format validation (international support)

EXIGENCES:
- Suivre CONTRIBUTING.md (coding standards)
- Validation côté backend (@Valid, @NotBlank, @Size)
- Exception handling (GlobalExceptionHandler)
- Tests unitaires (>80% coverage)
- Respecter TOUS les acceptance criteria des US001-005
- DTO pour API (pas d'exposition directe des entities)
- MapStruct pour mapping Entity ↔ DTO

ORDRE D'IMPLÉMENTATION:
1. Backend Entity (Building.java)
2. Backend Repository (BuildingRepository.java)
3. Backend DTOs (BuildingDTO, CreateBuildingRequest, UpdateBuildingRequest)
4. Backend Mapper (BuildingMapper.java)
5. Backend Service (BuildingService.java) + Tests
6. Backend Controller (BuildingController.java) + Tests
7. Frontend Model (building.model.ts)
8. Frontend Service (building.service.ts)
9. Frontend Components (list, form, details)
10. Frontend Routing

Commence par créer les fichiers backend dans l'ordre ci-dessus.
Pour chaque fichier, indique le chemin complet depuis la racine du projet.