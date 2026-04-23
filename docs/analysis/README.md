# ImmoCare - Analysis Documentation

## Overview

This directory contains all analysis and design documentation for the ImmoCare application - a property management system for buildings, housing units, and rental operations.

**Project Name**: ImmoCare  
**Version**: 1.0  
**Phase**: Phase 1 - Foundation  
**Last Updated**: 2024-01-15

---

## Purpose

ImmoCare is designed to manage:
- 🏢 **Buildings**: Physical buildings with addresses and ownership
- 🏠 **Housing Units**: Individual apartments/units within buildings
- 📐 **Rooms**: Individual rooms within housing units with types and surfaces
- ⚡ **Energy Performance**: PEB (Energy Performance Certificate) scores with history
- 💰 **Rent**: Indicative rent amounts with history
- 💧 **Utilities**: Water meters with history (electricity, gas, heating in future)

---

## Documentation Structure

### Core Documentation

| Document | Description | Status |
|----------|-------------|--------|
| [data-model.md](./data-model.md) | Complete data model with ERD diagram and entity descriptions | ✅ Ready |
| [data-dictionary.md](./data-dictionary.md) | Detailed specifications for all entities and attributes | ✅ Ready |
| [roles-permissions.md](./roles-permissions.md) | User roles and permission matrix | ✅ Ready |
| [backlog.md](./backlog.md) | Future features and product roadmap | ✅ Ready |

### Use Cases

| Use Case | Description | Status |
|----------|-------------|--------|
| [UC001-manage-buildings.md](./use-cases/UC001-manage-buildings.md) | CRUD operations for buildings | ✅ Ready |
| UC002-manage-housing-units.md | CRUD operations for housing units | 📋 TODO |
| UC004-manage-rooms.md | CRUD operations for rooms | 📋 TODO |
| UC005-manage-peb-scores.md | Manage PEB score history | 📋 TODO |
| UC006-manage-rents.md | Manage rent history | 📋 TODO |
| UC007-manage-water-meters.md | Manage water meter history | 📋 TODO |

### User Stories

Located in `user-stories/` directory (to be created)

| Story | Description | Status |
|-------|-------------|--------|
| UC005.001-create-building.md | Create a new building | 📋 TODO |
| UC005.002-edit-building.md | Edit building information | 📋 TODO |
| UC005.003-delete-building.md | Delete a building | 📋 TODO |
| ... | (More to be defined) | 📋 TODO |

---

## Key Entities

### 1. USER
Authentication and user management with role-based access control.

**Current Roles**: ADMIN  
**Future Roles**: PROPERTY_MANAGER, ACCOUNTANT, MAINTENANCE_STAFF, TENANT, VIEWER

---

### 2. BUILDING
Physical buildings containing housing units.

**Key Attributes**:
- Name
- Address (street, postal code, city, country)
- Owner name (optional)

---

### 3. HOUSING_UNIT
Individual apartments or units within buildings.

**Key Attributes**:
- Unit number
- Floor and landing
- Total surface
- Terrace (optional with surface and orientation)
- Garden (optional with surface and orientation)
- Owner name (inherits from building or override)

---

### 4. ROOM
Individual rooms within housing units.

**Room Types**: Living room, Bedroom, Kitchen, Bathroom, Toilet, Hallway, Storage, Office, Dining room, Other

---

### 5. PEB_SCORE_HISTORY
Energy performance certificates with historical tracking.

**PEB Scores**: A++, A+, A, B, C, D, E, F, G

---

### 6. RENT_HISTORY
Indicative rent amounts with time-based tracking.

**Features**: Effective from/to dates, current rent tracking

---

### 7. WATER_METER_HISTORY
Water meter assignments with historical tracking.

**Features**: Installation/removal dates, active meter tracking

---

## Technology Stack

Based on `technical-requirements.md` from reference project:

### Backend
- **Java 17** (LTS)
- **Spring Boot 3.x**
- **Spring Security** (authentication & authorization)
- **Hibernate / JPA** (ORM)
- **PostgreSQL 15+** (database)
- **Flyway** (database migrations)
- **Maven** (build tool)

### Frontend
- **Angular 17+**
- **TypeScript**
- **Angular Material** or **PrimeNG** (UI components)
- **RxJS** (reactive programming)

### Infrastructure
- **Docker** (containerization)
- **Docker Compose** (orchestration)
- **Nginx** (reverse proxy)

### Architecture
- **Mono-repository** (backend + frontend + docs)
- **API-First Design** (RESTful API)
- **Microservices-ready** (future consideration)

---

## Development Phases

### ✅ Phase 0: Analysis & Design (Current)
- Data model definition
- Use case identification
- User story creation
- Technical architecture review

### 📋 Phase 1: Foundation (Q1 2024)
**Scope**:
- User authentication (ADMIN role only)
- Building CRUD operations
- Housing Unit CRUD operations
- Room management
- PEB score history
- Rent history
- Water meter history
- Basic Angular UI

**Deliverables**:
- Working application with core features
- Docker deployment
- Basic documentation

### 📋 Phase 2: Tenant & Financial (Q2-Q3 2024)
**Scope** (from backlog):
- Tenant management
- Lease contracts
- Payment tracking
- Additional utility meters (electricity, gas, heating)
- Role expansion (PROPERTY_MANAGER, ACCOUNTANT)
- OAuth 2.0 migration

### 📋 Phase 3: Operations (Q4 2024 - Q1 2025)
**Scope** (from backlog):
- Maintenance management
- Document management
- Financial reporting
- Dashboard
- Email notifications

### 📋 Phase 4: Self-Service (Q2-Q3 2025)
**Scope** (from backlog):
- Tenant portal
- Advanced reporting
- Utility bill generation
- Mobile app consideration

---

## Design Principles

### 1. API-First
The REST API is the primary interface. Any frontend (Angular, React, mobile) can consume it.

### 2. Historization
Time-sensitive data (PEB, rent, meters) uses history tables with append-only pattern.

### 3. Simplicity First
Start simple (single owner, single role). Add complexity incrementally based on real needs.

### 4. Data Integrity
Use database constraints, foreign keys, and application-level validation.

### 5. Audit Trail
Track who created/modified data with timestamps and user references.

---

## Business Rules Summary

### Building
- Must have complete address
- Owner is optional
- Can contain multiple housing units
- Deletion cascades to units and all related data

### Housing Unit
- Must belong to a building
- Unit number unique within building
- Inherits owner from building unless overridden
- Total surface can be calculated or manually entered
- Can have terrace and/or garden with orientations

### Room
- Must belong to housing unit
- Surface is approximate (not legally binding)
- Sum of room surfaces helps calculate unit surface

### PEB Score
- Append-only history table
- Most recent score_date = current score
- Score values: A++ to G

### Rent
- Append-only history table
- Current rent has effective_to = NULL
- Historical rents have explicit date ranges
- Indicative only (not actual tenant payments)

### Water Meter
- Append-only history table
- Current meter has removal_date = NULL
- Meter periods don't overlap

---

## Next Steps

1. ✅ Review and validate data model
2. ✅ Review and validate use cases
3. 📋 Create detailed user stories with acceptance criteria
4. 📋 Set up development environment
5. 📋 Initialize project structure (mono-repo)
6. 📋 Create database schema with Flyway
7. 📋 Implement backend APIs
8. 📋 Implement frontend UI
9. 📋 Deploy with Docker Compose

---

## How to Use This Documentation

### For Product Owners / Stakeholders
1. Start with this README for overview
2. Review [data-model.md](./data-model.md) to understand entities
3. Check [backlog.md](./backlog.md) for future features
4. Review use cases to understand workflows

### For Developers
1. Read [data-model.md](./data-model.md) and [data-dictionary.md](./data-dictionary.md) thoroughly
2. Review [roles-permissions.md](./roles-permissions.md) for security implementation
3. Follow use cases and user stories for feature implementation
4. Reference technical-requirements.md (from reference project) for tech stack

### For Testers
1. Use user stories as test scenarios
2. Verify acceptance criteria
3. Test business rules from data model
4. Validate data integrity constraints

---

## Document Conventions

### Status Indicators
- ✅ **Ready**: Complete and reviewed
- 📋 **TODO**: Not yet created
- 🚧 **In Progress**: Currently being worked on
- ⚠️ **Review**: Needs review or clarification

### File Naming
- Use cases: `UC###-short-description.md`
- User stories: `US###-short-description.md`
- All lowercase, hyphens for spaces

### Markdown Style
- Use headers (#, ##, ###) for structure
- Use tables for structured data
- Use code blocks for examples
- Use emoji sparingly for visual cues

---

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2024-01-15 | 1.0 | Initial documentation creation | Development Team |

---

## References

- **Reference Project**: SandwichesMgt (technical-requirements.md)
- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Angular Documentation**: https://angular.io/docs
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/

---

## Contact & Feedback

For questions, clarifications, or suggestions about this documentation:
- Open an issue in the project repository
- Contact the development team
- Review and comment during sprint planning

---

**Document Status**: ✅ Ready for Review  
**Next Update**: After user stories creation
