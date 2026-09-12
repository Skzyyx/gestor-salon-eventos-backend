# Working Agreement — Gestor de Salones de Eventos Team

- Channels and response times:
  Main communication through private WhatsApp and Discord groups.
  Expected response time: within 5 hours on business days.
- Collaboration hours:
  Team sessions on Monday and Wednesday mornings, plus asynchronous communication the rest of the week.
- Criteria for asking for help:
  If someone is blocked on a technical issue for more than 30 minutes,
  they must share it in the team channel before continuing to try alone,
  providing screenshots, error messages, or context to help the team understand the problem.
- Responsibility for evidence:
  Each member documents their own contribution in
  `docs/evidence/sprint-XX.md` before the end of each sprint,
  strictly following the established documentation formats and templates.
  The leader checks that the file is complete before submission.
- Rules for decisions and disagreements:
  Technical decisions are discussed in the team channel;
  if there is no consensus within a reasonable time, the leader decides
  taking everyone's arguments into account. For version control,
  a minimum of 2 team members are required to review and approve
  each change before it can be merged into the main branch.

## Branching strategy: GitHub Flow

The team uses **GitHub Flow**: a single long-lived branch (`main`) plus short-lived
`feature/*` branches for every change, integrated through Pull Requests and deleted after merge.

- `main` is the source of truth and must always be in a working state
  (`mvn clean test` passes). Nobody pushes directly to `main`; it is protected on GitHub.
- Every change (feature, fix, docs, config) starts from an up-to-date `main`
  in its own branch:

  ```bash
  git switch main
  git pull origin main
  git switch -c feature/short-description
  ```

  bashbashbashbash
- Branch naming: `feature/<short-description>`, for every kind of change
  (new functionality, bug fixes, docs, configuration).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org/):
  `feat:`, `fix:`, `docs:`, `chore:`, `ci:`, `refactor:`, `test:`.
  Commit early and often; keep each commit focused on one thing.
- Push the branch and open a Pull Request against `main` as soon as there is something
  to discuss (a Draft PR is fine while the work is in progress).
  The PR description states what changed, why, and how it was verified.
- A PR can be merged only when:

  - at least 2 team members (other than the author) have approved it,
  - all quality gates (CI build, tests and future checks) are green,
  - the branch is up to date with `main` and has no conflicts.
- Merge with **Squash and merge** so `main` keeps one clean commit per PR,
  then delete the branch. Branches should live days, not weeks; if a branch
  falls behind, rebase or merge `main` into it before requesting review.
- Anything merged into `main` is considered deployable. Releases and deployments
  (from Sprint 3 onward) are always cut from `main`.



## Pull Requests

- Every PR uses the template in `.github/pull_request_template.md`. The five sections
  are mandatory: **What changes**, **Why**, **How it was verified**, **Risks and pending
  items**, and the **Working agreement checklist**. A section that does not apply is
  marked `N/A` with the reason; it is never deleted.
- Reviewers read "Risks and pending items" before approving. An empty section, or
  "none" without a reason, is a request for changes, not an approval.
- Quality gates required to merge, in addition to the 2 approvals:
  - the `Build` GitHub Actions check (Maven build and tests) is green;
  - the SonarQube quality gate for the PR is green. The gate and its thresholds are
    recorded in `docs/adr/0002-sonarqube-quality-gate.md` and can only change through
    a new ADR.
- The `historial` branch is a read-only archive of the exploratory work done before this
  process was followed. It is never merged, rebased, deleted or pushed to.
