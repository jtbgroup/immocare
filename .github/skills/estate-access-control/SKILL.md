---
name: estate-access-control
description: "Use when implementing or reviewing estate-scoped APIs, authorization, membership roles, resource ownership, or cross-estate data isolation."
---

# Estate Access Control and Data Isolation

Estate membership is a security boundary. Frontend route guards or filtering do not
replace backend authorization and ownership checks.

## API rules

1. Scope estate-owned API routes and operations with the estate identifier,
   following established `/api/v1/estates/{estateId}/...` routes.
2. Apply the existing Spring method-security convention: members may read where
   appropriate; manager-only operations require manager authorization. Check the
   role requirements for the specific operation rather than assuming all members
   have write access.
3. Validate that every referenced building, housing unit, person, asset, or other
   estate-owned resource belongs to the requested estate before reading or mutating
   it.
4. Do not trust an estate ID, owner ID, or related-resource ID supplied in a request
   body merely because the caller is authorized for some estate.
5. Use the existing exception and HTTP response conventions for not-found,
   forbidden, conflict, and validation failures.
6. Avoid leaking whether a resource exists in another estate unless the established
   API contract intentionally distinguishes that case.

## Verification

For new or changed estate-owned endpoints, cover:

- authorized member access to resources in their estate;
- manager-only write access and denial to non-manager members;
- denial of access to another estate's resource, including when its identifier is
  supplied in a nested request;
- platform-admin behavior where applicable.

Use Spring Boot integration tests and MockMvc for authorization and isolation
behavior. Verify both status codes and that no cross-estate data is returned or
changed.
