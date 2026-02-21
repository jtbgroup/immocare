# Use Case UC007: Manage Users

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC007 |
| **Use Case Name** | Manage Users |
| **Version** | 1.0 |
| **Status** | ✅ Ready for Implementation |
| **Priority** | HIGH - Foundation |
| **Actor(s)** | ADMIN |
| **Preconditions** | User must be authenticated with ADMIN role |
| **Postconditions** | User data is created, updated, or deleted in the system |
| **Related Use Cases** | None (Phase 1) |

---

## Description

This use case describes how an administrator manages user accounts in the ImmoCare system. In Phase 1, only the ADMIN role exists. All user management operations are restricted to authenticated ADMINs. There is no self-registration and no password reset by email.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator with full access
- **Goal**: Create, view, edit, and delete user accounts
- **Characteristics**:
  - Responsible for onboarding and offboarding system users
  - Knows the credentials and roles to assign
  - Cannot delete their own account
  - Cannot delete the last remaining ADMIN account

---

## Preconditions

1. User is logged in to the system
2. User has ADMIN role
3. System is operational and database is accessible

---

## Basic Flow

### 1. View All Users

**Trigger**: ADMIN navigates to the Users page

1. System displays a list of all user accounts
2. Each user shows:
   - Username
   - Email
   - Role
   - Created at (date)
3. ADMIN can sort the list:
   - Sort by username, email, or creation date
4. System provides pagination if more than 20 users

**Result**: ADMIN can see all users and navigate to details

---

### 2. View User Details

**Trigger**: ADMIN clicks on a specific user from the list

1. System displays detailed information:
   - User ID
   - Username
   - Email
   - Role
   - Created at (timestamp)
   - Updated at (timestamp)
2. ADMIN can:
   - Edit user information
   - Change user password
   - Delete user (subject to guard rules)

**Result**: ADMIN views complete user information

---

### 3. Create New User

**Trigger**: ADMIN clicks "Create User" button

1. System displays user creation form with fields:
   - **Username** (required, text, 3–50 chars, alphanumeric + underscore)
   - **Email** (required, valid email format, max 100 chars)
   - **Password** (required, min 8 chars, complexity rules)
   - **Confirm Password** (required, must match Password)
   - **Role** (required, dropdown, default: ADMIN — only ADMIN available in Phase 1)

2. ADMIN fills the form and clicks "Save"

3. System validates:
   - All required fields present
   - Username format and uniqueness
   - Email format and uniqueness
   - Password complexity
   - Password and Confirm Password match

4. System saves the user:
   - Generates user ID
   - Hashes password (BCrypt, strength 10)
   - Sets created_at and updated_at timestamps
   - Never stores plain-text password

5. System displays success message "User created successfully"

6. System redirects to user details page

**Result**: New user account is created and can log in

---

### 4. Edit User

**Trigger**: ADMIN clicks "Edit" on user details page

1. System displays edit form pre-filled with:
   - Username (editable)
   - Email (editable)
   - Role (editable, dropdown — only ADMIN in Phase 1)

2. Password is NOT shown and NOT editable here (see flow 5)

3. ADMIN modifies values and clicks "Save"

4. System validates (same rules as creation)

5. System updates the user record (updated_at refreshed)

6. System displays success message "User updated successfully"

**Result**: User information is updated

---

### 5. Change Password

**Trigger**: ADMIN clicks "Change Password" on user details page

1. System displays password change form:
   - **New Password** (required, min 8 chars, complexity rules)
   - **Confirm New Password** (required, must match)

2. ADMIN enters new password and clicks "Save"

3. System validates:
   - New password meets complexity requirements
   - Passwords match

4. System hashes and stores the new password

5. System invalidates any active session for that user (forces re-login)

6. System displays success message "Password changed successfully"

**Result**: User password is updated and existing sessions are terminated

---

### 6. Delete User

**Trigger**: ADMIN clicks "Delete" on user details page

1. System displays confirmation dialog:
   - Username shown in bold
   - "This action cannot be undone"
   - "Cancel" and "Delete" buttons

2. ADMIN clicks "Delete"

3. System checks guard rules before deletion:
   - Cannot delete own account
   - Cannot delete the last remaining ADMIN

4. System deletes the user

5. System redirects to users list

6. System displays success message "User deleted successfully"

**Result**: User account is permanently removed

---

## Alternative Flows

### Alternative Flow 1: Username Already Exists

**Step**: During create or edit, ADMIN submits a username already in use

1. System detects duplicate username
2. System displays error: "Username already exists"
3. Form remains with entered data
4. ADMIN must choose a different username

**Result**: User is not created/updated

---

### Alternative Flow 2: Email Already Exists

**Step**: During create or edit, ADMIN submits an email already in use

1. System detects duplicate email
2. System displays error: "Email already in use"
3. Form remains with entered data
4. ADMIN must provide a different email

**Result**: User is not created/updated

---

### Alternative Flow 3: Cancel Operation

**Step**: ADMIN clicks "Cancel" at any point during create or edit

1. System displays confirmation if form is dirty: "You have unsaved changes. Are you sure you want to cancel?"
2. ADMIN confirms cancellation
3. System discards changes and returns to user details or user list

**Result**: No changes are saved

---

## Exception Flows

### Exception Flow 1: Attempt to Delete Own Account

**Step**: ADMIN tries to delete their own account

1. System detects the user being deleted is the currently authenticated user
2. System displays error: "You cannot delete your own account"
3. Deletion is blocked

**Result**: Account is not deleted

---

### Exception Flow 2: Attempt to Delete Last ADMIN

**Step**: ADMIN tries to delete the only remaining user

1. System detects only one user account exists
2. System displays error: "Cannot delete the last administrator account"
3. Deletion is blocked

**Result**: Account is not deleted

---

### Exception Flow 3: Session Timeout

**Step**: User session expires during an operation

1. System detects expired session
2. System displays message: "Your session has expired. Please log in again."
3. System redirects to login page
4. ADMIN must re-authenticate and restart the operation

**Result**: Operation is not completed

---

## Business Rules

### BR-UC007-01: Required Fields
All users must have:
- Username
- Email
- Password (hashed)
- Role

**Rationale**: Minimum information needed to authenticate and authorize a user

---

### BR-UC007-02: Username Uniqueness
Usernames must be unique across all users (case-insensitive comparison recommended).

**Rationale**: Username is used as login identifier

---

### BR-UC007-03: Email Uniqueness
Email addresses must be unique across all users.

**Rationale**: Email is used for future password reset and notifications

---

### BR-UC007-04: Password Storage
Passwords are always stored as BCrypt hashes (strength 10). Plain-text passwords are never stored or logged.

**Rationale**: Security best practice

---

### BR-UC007-05: Self-Deletion Prevention
An ADMIN cannot delete their own account.

**Rationale**: Prevents accidental self-lockout

---

### BR-UC007-06: Last Admin Protection
The system must always retain at least one ADMIN account. Deletion of the last ADMIN is blocked.

**Rationale**: Prevents total loss of administrative access

---

### BR-UC007-07: Session Invalidation on Password Change
When a user's password is changed by an admin, all active sessions for that user are invalidated.

**Rationale**: Ensures the user re-authenticates with the new credentials

---

### BR-UC007-08: No Self-Registration
Users cannot create their own accounts. Only an authenticated ADMIN can create user accounts.

**Rationale**: Access to the system is controlled and provisioned by administrators only

---

## Data Elements

### Input Data
- Username (string, 3–50 chars, alphanumeric + underscore)
- Email (string, max 100 chars, valid email format)
- Password (string, min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit)
- Role (enum: ADMIN — only value in Phase 1)

### Output Data
- User ID (generated)
- Username
- Email
- Role
- Created at (timestamp)
- Updated at (timestamp)
- Password hash is NEVER returned in any response

---

## User Interface Requirements

### Users List Page
- Table view of users
- Columns: Username, Email, Role, Created At
- Sorting by any column
- Pagination (20 items per page)
- "Create User" button

### User Details Page
- User information in read-only format
- "Edit" button
- "Change Password" button
- "Delete" button (disabled with tooltip if self or last admin)

### User Form (Create/Edit)
- Form layout with labeled fields
- Required field indicators (asterisk *)
- Real-time field validation feedback
- "Save" and "Cancel" buttons

### Change Password Form
- New Password and Confirm Password fields only
- Password strength indicator (optional)
- "Save" and "Cancel" buttons

---

## Performance Requirements

- Users list must load in < 1 second (for up to 100 users)
- User details page must load in < 500ms
- Create/Edit/Delete operations must complete in < 1 second

---

## Security Requirements

### Authorization
- Only users with ADMIN role can access this use case
- All operations require authentication
- Session must be active

### Data Protection
- Password hash is never exposed through any API endpoint or UI
- User ID and credentials are not logged in application logs

### Audit Trail
- Record who created each user (created_by)
- Record when each user was created (created_at)
- Record when each user was last modified (updated_at)

---

## Test Scenarios

### TS-UC007-01: Create User Successfully
**Given**: ADMIN is logged in
**When**: ADMIN creates a user with valid username, email, password, and role
**Then**: User is saved, password is hashed, success message is shown

### TS-UC007-02: Create User - Duplicate Username
**Given**: User "john_doe" already exists
**When**: ADMIN tries to create another user with username "john_doe"
**Then**: Error "Username already exists" is displayed, user is not created

### TS-UC007-03: Create User - Duplicate Email
**Given**: Email "john@example.com" is already in use
**When**: ADMIN tries to create a user with the same email
**Then**: Error "Email already in use" is displayed, user is not created

### TS-UC007-04: Create User - Passwords Do Not Match
**Given**: ADMIN is on the user creation form
**When**: Password and Confirm Password fields differ
**Then**: Error "Passwords do not match" is displayed, user is not created

### TS-UC007-05: Edit User Successfully
**Given**: User exists
**When**: ADMIN changes the email and saves
**Then**: User is updated with the new email

### TS-UC007-06: Change Password
**Given**: User exists with active session
**When**: ADMIN changes the user's password
**Then**: Password is updated, user's session is invalidated

### TS-UC007-07: Delete User Successfully
**Given**: User exists and is not the last ADMIN or the current user
**When**: ADMIN confirms deletion
**Then**: User is deleted and removed from the list

### TS-UC007-08: Prevent Self-Deletion
**Given**: ADMIN is logged in as "admin_a"
**When**: ADMIN tries to delete "admin_a"
**Then**: Error "You cannot delete your own account" is displayed

### TS-UC007-09: Prevent Last Admin Deletion
**Given**: Only one user account exists
**When**: ADMIN tries to delete it
**Then**: Error "Cannot delete the last administrator account" is displayed

### TS-UC007-10: View Users List
**Given**: 3 users exist
**When**: ADMIN navigates to the Users page
**Then**: All 3 users are displayed with their username, email, role, and creation date

---

## Related User Stories

- **US031**: As an ADMIN, I want to view all users so that I can manage system access
- **US032**: As an ADMIN, I want to create a user so that I can grant system access
- **US033**: As an ADMIN, I want to edit a user so that I can update their information
- **US034**: As an ADMIN, I want to change a user's password so that I can reset access when needed
- **US035**: As an ADMIN, I want to delete a user so that I can revoke system access

---

## Notes

- Phase 1 has only the ADMIN role; the role dropdown is present for future extensibility
- No self-registration, no password reset by email (planned for Phase 2 with Keycloak)
- No profile self-service (users cannot edit their own profile in Phase 1)
- Future: OAuth2 / Keycloak will replace embedded user management (see roles-permissions.md)
- Future: Account lockout after failed login attempts (BACKLOG)
- Future: Password history to prevent reuse (BACKLOG)
- Future: Secure email-based password reset (BACKLOG)

---

**Last Updated**: 2024-01-15
**Version**: 1.0
**Status**: ✅ Ready for Implementation
**Next Review**: After Phase 1 completion
