---
name: add-business-rule
description: Add, update or reclassify a business rule in documentation/business/rules/. Use whenever the user mentions a business rule, a validation or constraint seen in the code, "document this rule", a rule ID (BR-...), a rule status (Confirmed, Needs business validation, Needs clarification), or when a code change introduces a new constraint, status transition or permission that should be documented. Also use when a stakeholder confirms or rejects an existing rule, even if they do not say "skill" or "rule".
---

# Add or update a business rule

Rules live in `documentation/business/rules/<area>.md` as a table. The goal: every
constraint visible in the code or stated by a stakeholder is documented once, with a
stable ID and an honest status.

## 1. Pick the area file and ID prefix

| Area | File | Prefix |
|---|---|---|
| Estate (members, managers, deletion) | `rules/estate.md` | `BR-EST-` |
| Financial transactions | `rules/financial-transactions.md` | `BR-TX-` |
| Equipment (boiler, fire extinguisher, meter) | `rules/equipment.md` | `BR-EQ-` |
| Lease and people on a lease | `rules/lease.md` | `BR-LEASE-` |
| Transaction import | `rules/import.md` | `BR-IMP-` |

If the file does not exist, create it with the same table layout and add it to
`documentation/README.md`.

Next ID = highest existing number in that file + 1, zero-padded to 3 digits.
**Never renumber or reuse an ID**, even for a deleted rule (mark it `Deprecated`).

## 2. Write the rule

- One rule = one sentence, observable and testable.
- State what the system does, not what it should do.
- Use the glossary vocabulary (`estate`, `housing unit`, `lease`, `MANAGER`…).
- No endpoints, class names or UI wording in the rule text; classes go in
  "Observed sources" at the bottom of the file.

Bad: `The createTransaction endpoint returns 400 if the lease is linked to an expense.`
Good: `A lease may only be linked to an income transaction.`

## 3. Choose the status (closed vocabulary)

| Status | Use when |
|---|---|
| `Confirmed` | A business stakeholder explicitly confirmed it (add who/when in a note). |
| `Needs business validation` | Inferred from code; plausible, nobody confirmed. |
| `Needs clarification` | The code behavior looks ambiguous or possibly accidental. |
| `Deprecated` | No longer applies; keep the row, explain in one line. |

Code-derived rules start as `Needs business validation` or `Needs clarification`.
Never write `Confirmed` without a stakeholder source.

## 4. Link the loose ends

- Ambiguity remains → add a concrete question to `backlog/open_questions.md`
  (grounded in the code, answerable yes/no or by a choice).
- The rule implies missing behavior → add an item to `backlog/backlog.md`, not to the
  business page.
- New term → add it to `business/glossary.md`.
- Page edited → update its `last_verified` and `verified_against` header.
- Capability changed → update `business/capabilities.md`.

## 5. Example

Row added to `rules/lease.md`:

```markdown
| BR-LEASE-004 | Two leases on the same housing unit may have overlapping periods; no check prevents it. | Needs clarification |
```

Matching entry added to `backlog/open_questions.md`:

```markdown
- Can lease periods for the same housing unit overlap? (see BR-LEASE-004)
```

## Checklist before finishing

- [ ] ID is new and in the right file
- [ ] Status is from the closed vocabulary
- [ ] No implementation details in the rule text
- [ ] Open question / backlog / glossary / capabilities updated if relevant
- [ ] Page header refreshed
