# ImmoCare - Product Backlog

## Overview

This document contains features, enhancements, and ideas for future development phases. Items are categorized by theme and prioritized based on business value and dependencies.

---

## Priority Levels

- **P0 - Critical**: Blocking for current phase, must be implemented
- **P1 - High**: Important for next phase, high business value
- **P2 - Medium**: Desirable, moderate business value
- **P3 - Low**: Nice to have, low priority

---

## Epic 1: Tenant Management

**Status**: Backlog (Phase 2)  
**Priority**: P1  
**Business Value**: HIGH - Core rental business functionality

### Features

#### BACKLOG-001: Tenant Entity
**Description**: Create tenant/renter entity with contact information

**Requirements**:
- Tenant personal information (name, email, phone)
- Emergency contact
- Identification documents (optional)
- Notes/remarks
- Relationship to housing unit

**Dependencies**: None

**Estimate**: Medium

---

#### BACKLOG-002: Lease Contract Management
**Description**: Manage formal rental agreements between owner and tenant

**Requirements**:
- Contract start and end dates
- Rental terms and conditions
- Deposit amount
- Linked to housing unit and tenant
- Contract status (active, expired, terminated)
- Document attachment (PDF contract)
- Renewal tracking

**Dependencies**: BACKLOG-001

**Estimate**: Large

---

#### BACKLOG-003: Multiple Tenants per Unit
**Description**: Support multiple tenants (roommates) in a single housing unit

**Requirements**:
- Multiple tenant relationships to one unit
- Primary tenant designation
- Shared responsibility or individual contracts
- Split payment tracking (if applicable)

**Dependencies**: BACKLOG-001, BACKLOG-002

**Estimate**: Medium

---

## Epic 2: Payment & Financial Management

**Status**: Backlog (Phase 2)  
**Priority**: P1  
**Business Value**: HIGH - Cash flow tracking and financial reporting

### Features

#### BACKLOG-004: Rent Payment Tracking
**Description**: Record actual rent payments received from tenants

**Requirements**:
- Payment date
- Amount paid
- Payment method (bank transfer, cash, check, etc.)
- Reference/transaction ID
- Link to tenant and housing unit
- Payment status (pending, received, late, partial)
- Payment history

**Dependencies**: BACKLOG-001, BACKLOG-002

**Estimate**: Large

---

#### BACKLOG-005: Charge Management
**Description**: Track additional charges beyond base rent

**Requirements**:
- Utility charges (water, electricity, gas, heating)
- Common area charges
- Parking fees
- Other recurring or one-time charges
- Automatic charge calculation
- Charge history

**Dependencies**: BACKLOG-004

**Estimate**: Medium

---

#### BACKLOG-006: Financial Reporting
**Description**: Generate financial reports and dashboards

**Requirements**:
- Income report (monthly, quarterly, yearly)
- Outstanding payments
- Occupancy rate
- Revenue per building/unit
- Expense tracking (linked to maintenance)
- Profit/loss statements
- Export to Excel/PDF

**Dependencies**: BACKLOG-004, BACKLOG-005

**Estimate**: Large

---

#### BACKLOG-007: Invoice Generation
**Description**: Automatically generate rent invoices for tenants

**Requirements**:
- Monthly recurring invoice generation
- Include base rent + charges
- PDF generation
- Email delivery
- Invoice numbering system
- Payment reminders

**Dependencies**: BACKLOG-004, BACKLOG-005

**Estimate**: Medium

---

## Epic 3: Maintenance Management

**Status**: Backlog (Phase 2-3)  
**Priority**: P2  
**Business Value**: MEDIUM - Operational efficiency

### Features

#### BACKLOG-008: Maintenance Request
**Description**: Track maintenance and repair requests

**Requirements**:
- Request description
- Priority (low, medium, high, urgent)
- Category (plumbing, electrical, carpentry, cleaning, etc.)
- Status (open, in progress, completed, cancelled)
- Linked to housing unit or building
- Reported by (tenant or admin)
- Assigned to (maintenance staff or vendor)
- Photos/attachments

**Dependencies**: None (can work with BACKLOG-001 for tenant requests)

**Estimate**: Large

---

#### BACKLOG-009: Work Order Management
**Description**: Formal work orders for maintenance tasks

**Requirements**:
- Work order number
- Scheduled date/time
- Actual date/time
- Labor hours
- Materials used
- Cost tracking
- Completion notes
- Before/after photos

**Dependencies**: BACKLOG-008

**Estimate**: Medium

---

#### BACKLOG-010: Vendor/Contractor Management
**Description**: Manage external service providers

**Requirements**:
- Vendor contact information
- Service categories
- Rating/performance tracking
- Contract documents
- Preferred vendor list
- Cost history

**Dependencies**: BACKLOG-008

**Estimate**: Small

---

#### BACKLOG-011: Preventive Maintenance Schedule
**Description**: Schedule recurring maintenance tasks

**Requirements**:
- Maintenance calendar
- Recurring tasks (annual boiler check, gutter cleaning, etc.)
- Automatic reminders
- Task completion tracking
- Linked to specific buildings or units

**Dependencies**: BACKLOG-008

**Estimate**: Medium

---

## Epic 4: Utility Meter Management

**Status**: Backlog (Phase 2)  
**Priority**: P1  
**Business Value**: MEDIUM - Complete utility tracking

### Features

#### BACKLOG-012: Electricity Meter Tracking
**Description**: Track electricity meters similar to water meters

**Requirements**:
- Meter number
- Installation/removal dates
- Meter readings
- Link to housing unit
- History

**Dependencies**: None (pattern exists with water meters)

**Estimate**: Small

---

#### BACKLOG-013: Gas Meter Tracking
**Description**: Track gas meters

**Requirements**: Same as BACKLOG-012

**Dependencies**: None

**Estimate**: Small

---

#### BACKLOG-014: Heating Meter Tracking
**Description**: Track heating meters (individual or shared)

**Requirements**: Same as BACKLOG-012

**Dependencies**: None

**Estimate**: Small

---

#### BACKLOG-015: Meter Reading Management
**Description**: Record and track meter readings over time

**Requirements**:
- Reading date
- Reading value
- Previous reading
- Consumption calculation
- Reading photos (optional)
- Anomaly detection (sudden spike)
- Export readings

**Dependencies**: BACKLOG-012, BACKLOG-013, BACKLOG-014

**Estimate**: Medium

---

#### BACKLOG-016: Utility Bill Generation
**Description**: Generate utility bills based on meter readings

**Requirements**:
- Consumption calculation
- Rate application (per kWh, m³, etc.)
- Bill generation
- PDF export
- Link to payment tracking

**Dependencies**: BACKLOG-015, BACKLOG-004

**Estimate**: Large

---

## Epic 5: Document Management

**Status**: Backlog (Phase 3)  
**Priority**: P2  
**Business Value**: MEDIUM - Centralized document storage

### Features

#### BACKLOG-017: Document Upload & Storage
**Description**: Store documents related to buildings, units, tenants, and maintenance

**Requirements**:
- File upload (PDF, images, Word, Excel)
- Document categorization (contract, invoice, certificate, photo, etc.)
- Link to relevant entity (building, unit, tenant, maintenance)
- Version control
- Access control
- Search and filter

**Dependencies**: None

**Estimate**: Medium

---

#### BACKLOG-018: Document Expiry Tracking
**Description**: Track documents with expiration dates

**Requirements**:
- Expiry date field
- Automatic reminders before expiry
- Expired document report
- Renewal workflow

**Dependencies**: BACKLOG-017

**Estimate**: Small

---

## Epic 6: Reporting & Analytics

**Status**: Backlog (Phase 3)  
**Priority**: P2  
**Business Value**: MEDIUM - Data-driven decision making

### Features

#### BACKLOG-019: Dashboard
**Description**: Interactive dashboard with key metrics

**Requirements**:
- Total buildings/units
- Occupancy rate
- Outstanding payments
- Maintenance requests status
- Revenue charts
- Customizable widgets

**Dependencies**: Various (BACKLOG-004, BACKLOG-008)

**Estimate**: Large

---

#### BACKLOG-020: Custom Reports
**Description**: Build custom reports with filters

**Requirements**:
- Report builder interface
- Filter by building, unit, date range, status, etc.
- Chart types (bar, line, pie)
- Export to PDF/Excel
- Save report templates

**Dependencies**: None

**Estimate**: Large

---

#### BACKLOG-021: Audit Trail Viewer
**Description**: View system audit logs

**Requirements**:
- Who did what, when
- Filter by user, entity, action
- Export audit logs
- Compliance reporting

**Dependencies**: None (audit logging should be in Phase 1)

**Estimate**: Small

---

## Epic 7: Communication

**Status**: Backlog (Phase 3)  
**Priority**: P3  
**Business Value**: LOW-MEDIUM - Improved communication

### Features

#### BACKLOG-022: Email Notifications
**Description**: Automatic email notifications

**Requirements**:
- Rent payment reminders
- Maintenance request updates
- Document expiry alerts
- Lease renewal reminders
- Configurable templates
- Unsubscribe option

**Dependencies**: BACKLOG-001, BACKLOG-004, BACKLOG-008

**Estimate**: Medium

---

#### BACKLOG-023: SMS Notifications
**Description**: SMS notifications for urgent matters

**Requirements**:
- SMS gateway integration
- Urgent maintenance alerts
- Payment reminders
- Configurable triggers

**Dependencies**: BACKLOG-022

**Estimate**: Small

---

#### BACKLOG-024: In-App Messaging
**Description**: Internal messaging system

**Requirements**:
- Chat between admin and tenants
- Maintenance staff communication
- Message history
- Read receipts

**Dependencies**: BACKLOG-001

**Estimate**: Large

---

## Epic 8: Tenant Portal

**Status**: Backlog (Phase 4)  
**Priority**: P2  
**Business Value**: MEDIUM - Tenant self-service

### Features

#### BACKLOG-025: Tenant Portal Login
**Description**: Self-service portal for tenants

**Requirements**:
- Tenant authentication
- Role: TENANT
- Access only to own data
- Mobile-responsive design

**Dependencies**: BACKLOG-001

**Estimate**: Medium

---

#### BACKLOG-026: Tenant Portal Features
**Description**: Features available in tenant portal

**Requirements**:
- View lease information
- View payment history
- Download invoices
- Submit maintenance requests
- View maintenance status
- Update contact information
- View announcements

**Dependencies**: BACKLOG-025, various

**Estimate**: Large

---

## Epic 9: Advanced Features

**Status**: Backlog (Phase 4+)  
**Priority**: P3  
**Business Value**: LOW - Advanced use cases

### Features

#### BACKLOG-027: Multi-Owner Support
**Description**: Support multiple property owners with data isolation

**Requirements**:
- Owner entity
- Data segregation by owner
- Owner-specific users
- Portfolio view per owner

**Dependencies**: Major refactoring

**Estimate**: X-Large

---

#### BACKLOG-028: Integration with External Systems
**Description**: Integrate with third-party systems

**Requirements**:
- Accounting software integration (Odoo, QuickBooks)
- Payment gateway integration (Stripe, PayPal)
- Email service integration (SendGrid, Mailchimp)
- Calendar integration (Google Calendar, Outlook)

**Dependencies**: Various

**Estimate**: Large (per integration)

---

#### BACKLOG-029: Mobile Application
**Description**: Native mobile app for iOS and Android

**Requirements**:
- Native app using React Native or Flutter
- Consume same REST API
- Push notifications
- Offline support (limited)

**Dependencies**: None (API already supports)

**Estimate**: X-Large

---

#### BACKLOG-030: Multi-Language Support
**Description**: Internationalization and localization

**Requirements**:
- Support multiple languages (EN, FR, NL, DE)
- Currency selection
- Date format localization
- Translatable content

**Dependencies**: None

**Estimate**: Large

---

#### BACKLOG-031: Advanced Search
**Description**: Full-text search across all entities

**Requirements**:
- Search buildings, units, tenants, documents
- Elasticsearch integration
- Faceted search
- Search suggestions

**Dependencies**: None

**Estimate**: Medium

---

#### BACKLOG-032: Calendar View
**Description**: Calendar interface for events

**Requirements**:
- Lease start/end dates
- Maintenance scheduled dates
- Payment due dates
- Document expiry dates
- Monthly/weekly/daily view

**Dependencies**: Various

**Estimate**: Medium

---

## Epic 10: System Administration

**Status**: Backlog (Phase 2+)  
**Priority**: P2  
**Business Value**: MEDIUM - System management

### Features

#### BACKLOG-033: System Configuration UI
**Description**: Admin interface for system settings

**Requirements**:
- Configure email settings
- Configure currency
- Configure date format
- Configure payment methods
- Configure notification preferences
- Configure backup schedule

**Dependencies**: None

**Estimate**: Medium

---

#### BACKLOG-034: Data Import/Export
**Description**: Bulk import and export of data

**Requirements**:
- Import from Excel/CSV
- Export to Excel/CSV/PDF
- Data validation on import
- Error reporting
- Templates for import

**Dependencies**: None

**Estimate**: Medium

---

#### BACKLOG-035: Backup & Restore
**Description**: Manual and automated backup functionality

**Requirements**:
- Manual backup trigger
- Scheduled automatic backups
- Restore from backup
- Backup to cloud storage (optional)
- Backup encryption

**Dependencies**: None

**Estimate**: Small

---

## Technical Debt & Improvements

### BACKLOG-036: OAuth 2.0 / Keycloak Migration
**Priority**: P1  
**Description**: Migrate from embedded authentication to Keycloak  
**Estimate**: Large

### BACKLOG-037: Automated Testing Suite
**Priority**: P1  
**Description**: Comprehensive unit, integration, and E2E tests  
**Estimate**: X-Large (ongoing)

### BACKLOG-038: Performance Optimization
**Priority**: P2  
**Description**: Database query optimization, caching layer (Redis)  
**Estimate**: Medium

### BACKLOG-039: Kubernetes Deployment
**Priority**: P2  
**Description**: Migrate from Docker Compose to Kubernetes  
**Estimate**: Large

### BACKLOG-040: API Versioning
**Priority**: P2  
**Description**: Implement proper API versioning strategy  
**Estimate**: Medium

---

## Prioritization Matrix

| Epic | Priority | Business Value | Effort | Dependencies | Phase |
|------|----------|----------------|--------|--------------|-------|
| Tenant Management | P1 | HIGH | Large | None | 2 |
| Payment & Financial | P1 | HIGH | Large | Tenant Mgmt | 2 |
| Utility Meters (all) | P1 | MEDIUM | Medium | None | 2 |
| OAuth Migration | P1 | MEDIUM | Large | None | 2 |
| Maintenance Management | P2 | MEDIUM | Large | None | 2-3 |
| Document Management | P2 | MEDIUM | Medium | None | 3 |
| Reporting & Analytics | P2 | MEDIUM | Large | Financial | 3 |
| Tenant Portal | P2 | MEDIUM | Large | Tenant Mgmt | 4 |
| Communication | P3 | LOW-MEDIUM | Medium | Tenant Mgmt | 3 |
| Advanced Features | P3 | LOW | X-Large | Various | 4+ |

---

## Roadmap Suggestion

### Phase 1 (Current - Q1 2024)
- Core building and housing unit management
- Basic authentication (ADMIN role)
- PEB scores, rents, water meters

### Phase 2 (Q2-Q3 2024)
- Tenant management (BACKLOG-001, 002, 003)
- Payment tracking (BACKLOG-004, 005, 007)
- All utility meters (BACKLOG-012, 013, 014, 015)
- Roles expansion (PROPERTY_MANAGER, ACCOUNTANT)
- OAuth 2.0 migration (BACKLOG-036)

### Phase 3 (Q4 2024 - Q1 2025)
- Maintenance management (BACKLOG-008, 009, 010, 011)
- Document management (BACKLOG-017, 018)
- Financial reporting (BACKLOG-006)
- Basic dashboard (BACKLOG-019)
- Email notifications (BACKLOG-022)

### Phase 4 (Q2-Q3 2025)
- Tenant portal (BACKLOG-025, 026)
- Advanced reporting (BACKLOG-020)
- Utility bill generation (BACKLOG-016)
- Mobile application consideration (BACKLOG-029)

### Phase 5+ (2026+)
- Multi-owner support (BACKLOG-027)
- External integrations (BACKLOG-028)
- Advanced features (BACKLOG-030, 031, 032)

---

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: Living Document  
**Next Review**: After Phase 1 completion
