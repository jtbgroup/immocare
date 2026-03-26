# Information baseline

The general organization of the project is defined in the `project_config/project_requirements.md` file. This file is project agnostic and gives a standard structure for the project.

The specific information of the project is defined in the `project_config/project_definition.md` file. THis file is more business oriented, specifically for the project but may also contain specific technical requirements.

Those files must be well understood at anytime of the project.

# AI behaviour

* All the generated documents must be produced in english
* Never generate any document without a consent; always explain before generating and ask confirmation.

# Startup behavior
At the beginning of EVERY conversation, Claude MUST:
1. Read `project_config/project_requirements.md`
2. Read `project_config/project_definition.md`
3. Confirm explicitly that both files have been read before doing anything else.