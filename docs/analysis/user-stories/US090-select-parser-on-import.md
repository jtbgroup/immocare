# User Story US090: Select Parser on Import

| Attribute | Value |
|-----------|-------|
| **Story ID** | US090 |
| **Epic** | Financial Management |
| **Related UC** | UC015 |
| **Priority** | MUST HAVE |
| **Story Points** | 1 |

**As an** ADMIN **I want to** select the appropriate parser before uploading a bank export **so that** the file is interpreted correctly regardless of the bank or format.

## Acceptance Criteria

**AC1:** Import form (US084) shows a "Parser" dropdown as the first field, listing all active parsers ordered by label. Each entry displays: label, format badge (CSV / PDF), bank hint (e.g. "Keytrade").

**AC2:** Selecting a parser restricts the file upload zone to the matching extension (`.csv` or `.pdf`). Uploading a file with the wrong extension → error "This parser expects a {FORMAT} file. Got: .{ext}."

**AC3:** The selected `parserCode` is sent as a multipart field (`parserCode`) in both the preview and import requests.

**AC4:** Unknown or inactive `parserCode` sent to backend → HTTP 400 "Parser not found or inactive."

**AC5:** If no active parser exists → import tab shows "No parsers available. Contact your administrator." and the Preview button is disabled.

**Endpoint:** `GET /api/v1/import-parsers` — returns active parsers only by default (`?activeOnly=true`).

**Last Updated:** 2026-03-10 | **Status:** ✅ Implemented
