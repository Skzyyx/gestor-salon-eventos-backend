\# ADR-0004: Frontend and Lambdas in this repository, backend at the root



\## Context



The React frontend lives in the private repository `Skzyyx/GestorSalonEventos`, next to an

older copy of the backend. The course grades this public repository, and the next units add

AWS Lambda functions that the frontend calls directly (image uploads, week 9). With two

repositories, a change to that contract needs two coordinated pull requests, and the

professor cannot see half of it.



The backend's pipeline assumes the backend at the root: `ci.yml` and its required `Build`

check, the JaCoCo gate, the SonarQube project, the `Dockerfile`, the README commands and the

evidence links.



\## Decision



\- \*\*One repository.\*\* The frontend moves to `frontend/`. AWS Lambda functions will live in

&#x20; `lambdas/<name>/`, each with its own `pom.xml` (not modules of the backend's pom), and

&#x20; Terraform, when the course reaches it, in `infra/`.

\- \*\*The backend stays at the root.\*\* Nothing in its build, CI, SonarQube or Docker setup

&#x20; changes.

\- \*\*Import without history.\*\* Only the versioned files of `frontend/` at commit `4f9a488` of

&#x20; the private repository were copied, with `git archive`: no `node\_modules`, `dist` or local

&#x20; `.env` files.

\- \*\*Separate frontend checks.\*\* The frontend gets its own workflow, triggered only by changes

&#x20; under `frontend/`. `ci.yml` keeps running on every change, so the required `Build` check

&#x20; always reports.

\- \*\*Configuration by environment.\*\* The API base URL comes from `VITE\_API\_URL`. Everything

&#x20; that starts with `VITE\_` is public (it ends up in the bundle), so no secret goes there.

\- \*\*The private repository is archived\*\* (read only, not deleted) once the frontend checks

&#x20; are green on `main`.



\## Alternatives considered



\- \*\*Keep two repositories.\*\* Rejected: the professor only sees this one, and the upload

&#x20; contract would change in two places.

\- \*\*Move the backend to `backend/`.\*\* Rejected for now: it breaks the paths in `ci.yml`,

&#x20; SonarQube, the `Dockerfile`, the README and the evidence right before the Lambda

&#x20; deliverables. It can be revisited with its own ADR.

\- \*\*Import with history (`git subtree` or `git filter-repo`).\*\* Rejected: the private history

&#x20; mixes the frontend with an old copy of the backend. It stays readable in the archived

&#x20; repository.



\## Trade-offs / consequences



\- The frontend's history is not in this repository; it remains in the archived one.

\- A frontend-only pull request still runs the backend `Build` (3 to 6 minutes), because

&#x20; `ci.yml` has no path filters. Accepted, so the required check always reports.

\- SonarQube's Maven scanner does not analyse `frontend/`; lint, typecheck and build are the

&#x20; frontend's checks for now.

\- `.dockerignore` excludes `frontend/`, `lambdas/` and `infra/`, so they never enter the

&#x20; backend image (`frontend/node\_modules` alone is about 170 MB).



\## Validation evidence



\- Pull request that imports the frontend: #\[n].

\- Frontend workflow: added in the next pull request (T3 of the team guide).

