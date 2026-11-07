# Copilot instructions

These instructions apply to all AI-assisted work in this repository. Keep this file
short; put language-specific conventions in `.github/instructions/`, role guidance
in `.github/agents/`, and repeatable procedures in `.github/skills/`.

## Choose the right role and guidance

- Backend implementation and tests: use the Java backend agent.
- Frontend implementation and tests: use the Angular frontend agent.
- For changes spanning backend API contracts and frontend clients, use both the Java
  backend and Angular frontend agents as needed; load the
  [`api-contract-sync`](./skills/api-contract-sync/SKILL.md) skill.
- Analysis or maintenance of canonical documentation: use the documentation
  maintainer. It must not change application source code.
- New focused Q&A roles can be added as separate agents in `.github/agents/`.
- Load the relevant skill for a repeatable task; do not copy its full procedure into
  another agent or instruction file.

## Project-wide expectations

- Before changing code, identify related behavior, conventions, tests, and existing
  documentation. Preserve established behavior unless the task explicitly changes it.
- After every code change, assess whether it changes documented business behavior,
  API contracts, persisted data, or technical behavior. Update the affected canonical
  pages and their links when needed; explain when no documentation change is needed.
- Canonical documentation is in English and must follow
  [`documentation/methodology.md`](../documentation/methodology.md). Keep each fact
  in one authoritative place and link to it instead of duplicating it.
- Follow the confirmed stack and deployment targets in
  [`technology-stack-and-deployment.md`](../documentation/technical/technology-stack-and-deployment.md).
- Do not introduce deprecated APIs or syntax. When touching deprecated code, replace
  it within the affected scope when safe; do not start an unrelated repository-wide
  rewrite.
- For a domain question, start with
  [`documentation/business/capabilities.md`](../documentation/business/capabilities.md)
  and then consult the glossary.
- When adding, changing, or reclassifying a business rule, follow the
  [`add-business-rule` skill](./skills/add-business-rule/SKILL.md).
- Run the most relevant existing tests and report the exact validation performed.
