---
name: maintain-documentation
description: "Analyze code changes and update canonical project documentation, formatting, metadata, source references, navigation, and Markdown cross-links. Use after behavior, API, or data-model changes and for documentation audits."
---

# Maintain Canonical Documentation

Use this workflow whenever code changes may affect documented behavior, and whenever
canonical documentation is created, edited, or reviewed.

## Procedure

1. Read [`documentation/methodology.md`](../../../documentation/methodology.md) and
   [`documentation/README.md`](../../../documentation/README.md). Consult the
   glossary and relevant canonical pages before adding content.
2. Inspect the changed implementation and relevant tests. Record only behavior
   supported by code or explicit business confirmation. Mark code-inferred business
   rules with the status required by the methodology and
   [`add-business-rule`](../add-business-rule/SKILL.md).
3. Identify the authoritative page for each affected fact:
   - domain terms and relationships belong in business references;
   - current enforced constraints belong in business rules;
   - APIs, persistence, and implementation behavior belong in technical references;
   - unresolved decisions and future work belong in the corresponding backlog files.
4. Update existing pages where possible. If a new canonical page is necessary, add
   it to the documentation index and link it from related pages. Avoid duplicating
   full definitions across pages.
5. Preserve the project's Markdown conventions: English prose, YAML metadata before
   a descriptive `#` heading, and applicable `status`, `last_verified`, and
   `verified_against` fields.
6. Maintain navigation and cross-links in both directions where useful: update
   incoming links when a page moves or its purpose changes, add links from related
   canonical pages to new content, and repair links made stale by code or document
   changes. Use relative Markdown links for documentation pages and stable
   repository-relative source links when they help readers verify implementation.
7. Refresh `last_verified` and `verified_against` when verification metadata applies.
   Do not claim code verification if the relevant implementation was not inspected.
8. Review the rendered Markdown structure, link targets, index entries, and any
   references to renamed files. Report changed pages, evidence reviewed, and any
   unresolved business question. If documentation is unaffected, state why.

## Do not

- Treat an inferred implementation detail as confirmed business intent.
- Create duplicate definitions where a stable ID or link to a canonical page is
  sufficient.
- Add speculative future behavior to canonical business documentation.
- Leave links to removed or renamed documentation paths.
