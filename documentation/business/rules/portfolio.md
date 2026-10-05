# Candidate Business Rules — Portfolio

These rules describe constraints visible in the current implementation. They **need business validation**: a technical constraint does not prove that it is an intended business policy.

| ID | Observed rule | Status |
|---|---|---|
| BR-PORT-001 | A portfolio name must be unique, ignoring case. | Needs business validation |
| BR-PORT-002 | When portfolio creation provides a non-empty member list, that list must include at least one `MANAGER`. The service accepts an empty list. | Needs clarification |
| BR-PORT-003 | The last `MANAGER` of a portfolio cannot be demoted or removed. | Needs business validation |
| BR-PORT-004 | The observed service does not allow a portfolio containing at least one building to be deleted. | Needs business validation |
| BR-PORT-005 | A user cannot be added to the same portfolio twice. Duplicate user IDs in the initial creation list are ignored. | Needs clarification |

## Observed sources

`EstateService`, `Estate`, `EstateMember`, and `EstateRole`. The manager-presence rule applies only when a non-empty member list is supplied; do not generalize it without validation.
