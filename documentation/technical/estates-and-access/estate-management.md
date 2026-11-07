---
title: Estate Management
type: technical reference
status: current
last_verified: 2026-10-28
verified_against: "EstateController, EstateService, BoilerRepository, FireExtinguisherRepository, EstateConfigService, AlertService, AlertController"
---

# Estate Management

This page documents current estate administration, estate selection, membership, dashboard, and access-control behavior in the backend and frontend. Domain concepts are described in [Estate and Access](../../business/estates-and-access/concepts/estate-and-access.md); canonical rule statements and statuses are in the [estate rules](../../business/estates-and-access/rules/estate.md).

## Access model

All estate API calls require authentication. The backend applies `isPlatformAdmin()` for platform-wide estate administration, `isMemberOf(estateId)` for member reads, and `isManagerOf(estateId)` for management operations. Platform administrators pass both estate checks without an estate membership. Other users need a matching `estate_member` row; managers can manage, and viewers have read access.

Estate-scoped backend routes carry the estate UUID explicitly in the URL. The active estate is held in an in-memory Angular signal and is not stored in the server session or persisted by the frontend. On entry, the frontend checks the requested estate against the active estate; when no estate is active, it loads the user's available estates and may redirect to the selector. Restoring the same estate and page after a browser reload is a confirmed product requirement and is tracked in the [backlog](../../../backlog/backlog.md).

## API behavior

| Operation | Endpoint | Access | Current behavior |
|---|---|---|---|
| List all estates | `GET /api/v1/admin/estates` | Platform administrator | Paginated; defaults to page `0`, size `20`, sort `name,asc`; optional partial case-insensitive name search. |
| Create estate | `POST /api/v1/admin/estates` | Platform administrator | Creates estate and optional initial memberships in one service transaction; returns `201` and an estate DTO. |
| Delete estate | `DELETE /api/v1/admin/estates/{id}` | Platform administrator | Returns `204` if deletion succeeds; rejected with `409` if the estate contains buildings. Other linked-data constraints may still prevent deletion (see known gaps below). |
| List available estates | `GET /api/v1/estates/mine` | Any authenticated user | Ordinary users receive their memberships and roles; platform administrators receive every estate with `myRole = null`. |
| Read estate | `GET /api/v1/estates/{id}` | Manager or platform administrator | Returns the estate DTO. |
| Update estate metadata | `PUT /api/v1/estates/{id}` | Manager or platform administrator | Updates name and description. There is no separate admin-prefixed update endpoint; the admin edit screen uses this estate-scoped endpoint. |
| Read dashboard | `GET /api/v1/estates/{id}/dashboard` | Member or platform administrator | Returns building, housing-unit, active-lease, and pending-alert counts. |
| List members | `GET /api/v1/estates/{id}/members` | Manager or platform administrator | Returns members ordered by username. |
| Add member | `POST /api/v1/estates/{id}/members` | Manager or platform administrator | Adds an existing user with `MANAGER` or `VIEWER`; returns `201`. |
| Change member role | `PATCH /api/v1/estates/{id}/members/{userId}` | Manager or platform administrator | Changes the target membership role. |
| Remove member | `DELETE /api/v1/estates/{id}/members/{userId}` | Manager or platform administrator | Removes the membership; returns `204`. |

For membership operations, estate-not-found and user-not-found errors return `404`; duplicate membership, last-manager, and self-operation conflicts return `409`; estate access denial returns `403`. Estate name conflicts return `409`.

## Frontend flows

- The estate selector and estate switcher use `GET /api/v1/estates/mine`. One result is selected automatically; multiple results are shown for selection. A platform administrator can also open the platform estate list.
- The platform estate list supports server-side search and pagination. The estate form supports creation with an optional, searchable initial member list and editing of estate metadata.
- In edit mode, the form has separate general-information and member-management sections. A standalone estate-member page also supports adding members, editing roles, and removing memberships.
- The dashboard displays totals for buildings, housing units, active leases, and pending boiler, fire-extinguisher, lease-end, and indexation alerts. A boiler alert is counted when its next service date is before the estate's configured warning threshold (default three months). A fire extinguisher is counted when it has no revision or its latest revision is more than one year old; that fixed inspection interval is not configurable and remains subject to business clarification ([BR-EQ-031](../../business/equipment/rules/equipment.md)). It links to estate features and shows member navigation and estate-edit actions for managers and platform administrators, but not viewers.
- Bank-transaction parsers are global, active parser options are available to authenticated users, and any estate can use them for imports. Transaction preview and import operations are estate-scoped; the chosen estate is supplied in the route, and imported transactions are created for that estate. Parsers are not managed as estate-specific settings, and there is no separate parser-registry management page in the current frontend.

## Differences and known gaps

- The legacy `UC003.005-assign-first-manager` story describes a single `firstManagerId`; current create requests accept a `members` list. When that list is empty, the service permits estate creation without any manager; when non-empty, the submitted list must contain a manager. This violates the confirmed invariant that every estate has a manager (BR-ESTATE-008).
- The two historical `UC003.005` stories conflict in title and scope. The current implemented behavior is the members-list flow described above.
- Duplicate user IDs in the initial members list are skipped by the backend, while the create form prevents adding duplicates before submission. The confirmed rule requires rejecting duplicates explicitly (BR-ESTATE-009).
- The member-management components search users through `GET /api/v1/users`, which only allows platform administrators. A manager can therefore fail to find users; the component currently converts that error into an empty result list.
- The edit form declares a current-user ID for its inline self-operation checks but does not populate it. The standalone members page does resolve the current user. Backend checks still reject self-role changes and self-removal (BR-ESTATE-006 and BR-ESTATE-007).
- After estate creation, the form ignores the returned estate DTO and navigates using its unset edit-mode estate ID; creation therefore does not navigate to the new estate dashboard.
- The delete service checks for buildings, then deletes the estate. Other foreign keys, including person and estate-configuration references, do not all cascade on estate deletion; deleting a building-free estate with linked records may fail at the database rather than returning the documented building conflict. The confirmed policy is to block deletion while any estate-scoped data remains (BR-ESTATE-010).
- Deleting a user account cascades its estate memberships. If the account is the last manager of an estate, this can leave the estate without a manager despite the confirmed invariant (BR-ESTATE-008).
- Some estate routes rely on backend authorization after the route guard activates. The client guard does not consistently enforce manager-only page access before rendering management screens.
- The cross-domain alert endpoints require authentication but do not verify membership in the requested estate before aggregating lease, boiler, and PEB alerts. An authenticated user can request alerts for another estate; see the [backlog](../../../backlog/backlog.md).
- A platform administrator's membership roles are represented as `null` in the available-estate response, even if an explicit membership row exists.

These behavior gaps are tracked in the [backlog](../../../backlog/backlog.md). The remaining estate decisions concern the scope of data transferred with a building and how to represent an explicit estate membership for a platform administrator; see [open questions](../../../backlog/open_questions.md).

## Implementation references

- Backend estate operations, manager/member access, and cross-domain alert access: [EstateController](../../../backend/src/main/java/com/immocare/controller/EstateController.java), [EstateAdminController](../../../backend/src/main/java/com/immocare/controller/EstateAdminController.java), [EstateService](../../../backend/src/main/java/com/immocare/service/EstateService.java), [EstateSecurityService](../../../backend/src/main/java/com/immocare/security/EstateSecurityService.java), [AlertController](../../../backend/src/main/java/com/immocare/controller/AlertController.java), and [AlertService](../../../backend/src/main/java/com/immocare/service/AlertService.java).
- Persisted model: [cross-cutting data model](../cross-cutting/data-model.md) and [Estate Data Dictionary](./estate-data-dictionary.md).
- Frontend routing and selection: [app.routes.ts](../../../frontend/src/app/app.routes.ts), [EstateGuard](../../../frontend/src/app/core/auth/estate.guard.ts), [ActiveEstateService](../../../frontend/src/app/core/services/active-estate.service.ts), and [EstateService](../../../frontend/src/app/core/services/estate.service.ts).
- Frontend administration: [estate management components](../../../frontend/src/app/features/estate/).
