# AI customization

Project-specific Copilot configuration is organized by purpose:

- [`copilot-instructions.md`](./copilot-instructions.md) contains concise,
  repository-wide expectations and routing guidance.
- [`agents/`](./agents/) contains focused backend, frontend, and documentation
  roles. Add future Q&A roles here as separate agents with a narrow scope and a
  discovery-oriented description. Cross-stack API changes are handled by the
  backend and frontend roles together, using the `api-contract-sync` skill.
- [`instructions/`](./instructions/) contains file-scoped conventions that are
  automatically applied to matching source and documentation files.
- [`skills/`](./skills/) contains on-demand procedures for repeatable work. Each
  skill lives in its own directory and has a `SKILL.md` entry point.

Keep the distinction clear: instructions define applicable conventions, agents
define who performs a task and its boundaries, and skills define how to complete a
repeatable workflow. Do not copy the same detailed procedure into multiple files.

Other repository assets under `.github/`, such as workflows, pull request templates,
and modernization artifacts, remain in their purpose-specific locations.
