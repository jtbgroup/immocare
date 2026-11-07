---
description: "Use when editing canonical Markdown documentation, business references, technical references, or backlog documents."
name: "Canonical documentation conventions"
applyTo: ["documentation/**/*.md", "backlog/**/*.md"]
---

# Canonical documentation conventions

- Follow [`documentation/methodology.md`](../../documentation/methodology.md);
  canonical documentation is written in English.
- Use Markdown for canonical project documentation. Keep business and technical
  references under `documentation/`; existing use-case, user-story, and prompt files
  under `docs/` are retained historical artifacts, not required deliverables for new
  work.
- Preserve the document's single purpose and authoritative home. Link to canonical
  definitions rather than duplicating them.
- Canonical pages start with YAML metadata, followed by a descriptive Markdown
  heading. Refresh `last_verified` and `verified_against` when applicable.
- When a new page is added, link it from the documentation index and add links to
  related canonical pages. When code behavior or structure changes, review and
  repair affected links and source references.
- Load the [`maintain-documentation`](../skills/maintain-documentation/SKILL.md)
  skill for documentation changes and the
  [`add-business-rule`](../skills/add-business-rule/SKILL.md) skill for business-rule
  changes.
