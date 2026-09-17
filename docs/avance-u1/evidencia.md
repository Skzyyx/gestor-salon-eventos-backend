# Unit 1 deliverable: pipeline with GitHub Actions and SonarQube

Complements [Sprint 1 evidence](../evidence/sprint-01.md), which records how `main` was
rebuilt through 8 PRs, and [ADR-0002](../adr/0002-sonarqube-quality-gate.md), which records
the quality gate. Nothing from those files is repeated here.

| Evidence asked for         | Where it is                                                                                                                                                                                      |
| :------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Repository and workflow    | "Identificación"                                                                                                                                                                                 |
| Reproducible configuration | [README, "Continuous integration and code quality"](../../README.md#-continuous-integration-and-code-quality)                                                                                    |
| Pull request               | [PR #11](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/11) (controlled failure and fix) and [PR #12](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/12) (this report) |
| Runs before and after      | "Observación" and "Corrección"                                                                                                                                                                   |
| Sonar result               | "Observación"                                                                                                                                                                                    |
| Technical decision         | "Decisión"                                                                                                                                                                                       |
| Individual participation   | "Participación individual"                                                                                                                                                                       |
| Traceability               | "Trazabilidad"                                                                                                                                                                                   |
| SonarQube hosting          | "Servidor de SonarQube" (asked by the professor by message, not in the PDF)                                                                                                                      |

## Identificación

- Team: Gestor de Salones de Eventos Team. José Luis Islas Molina (leader), Freddy Alí Castro
  Román, José Eduardo Aguilar García, Christopher Álvarez Centeno.
- Project: `gestor-salon-eventos-backend`, Java 17, Spring Boot 3.4.1, Maven.
- Repository: https://github.com/Skzyyx/gestor-salon-eventos-backend (public).
- Workflow: [`.github/workflows/build.yml`](../../.github/workflows/build.yml). Job and required
  status check: `Build`.
- Delivery commit: the squash commit of PR #13 on `main` (PR #12 delivered this report;
  PR #13 only put the README headings in English so that the link in the table above
  resolves). Its hash is reported in the submission message because a file cannot contain
  its own hash. State of `main` before this deliverable: `fe57fbe` (PR #10); after PR #11:
  `72dc3e4`; after PR #12: `ae4ac2e`.

## Flujo

- **Events.** `push` to `main` and `pull_request` targeting `main`. No manual trigger.
- **Runner.** GitHub-hosted `ubuntu-latest`.
- **Steps.** (1) `actions/checkout@v4` with `fetch-depth: 0`, full history so Sonar can compute
  new code and blame; (2) `actions/setup-java@v4`, Temurin 17; (3) and (4) `actions/cache@v4`
  for `~/.sonar/cache` and `~/.m2`; (5) "Build, test and analyze": `mvn -B verify` compiles,
  runs the tests with the JaCoCo agent and writes `target/site/jacoco/jacoco.xml`, then the
  same Maven invocation runs the `sonar-maven-plugin:5.8.0.7211:sonar` goal. On a
  `pull_request` event it passes `sonar.pullrequest.key/branch/base`; on a `push` it passes
  `sonar.branch.name`.
- **Sonar's role.** The scanner uploads sources, issues and the coverage report; the server
  evaluates the quality gate `gestor-salon-sprint1` on the new code of that PR or branch.
  Because of `-Dsonar.qualitygate.wait=true` the Maven goal waits for the verdict and fails
  when the gate fails, so a red gate becomes a red `Build` check.
- **What blocks a merge.** The ruleset on `main` requires the `Build` check, 2 approvals,
  squash as the only merge method and the branch up to date with `main`
  (`capturas/u1-ruleset-main.png`). The `historial` branch is locked by its own ruleset: no
  updates, no force pushes, no deletion (`capturas/u1-ruleset-historial.png`).
- **Secrets.** `SONAR_HOST_URL` and `SONAR_TOKEN` as repository secrets; `GITHUB_TOKEN` is
  provided by Actions. Names in the README and in `capturas/u1-secrets.png`, values nowhere.

## Predicción

Controlled failure chosen for this deliverable and proposed to the professor before PR #11
was opened (2026-09-16): a new class `mx.gestorsalon.service.CotizacionCalculator` (26 lines
to cover and 18 branches according to JaCoCo) added without tests in the first commit of
[PR #11](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/11).

- Expected: `mvn verify` passes (37 tests), the Sonar analysis completes and the gate fails
  on **Coverage on New Code = 0.0%** (threshold 50%). Step "Build, test and analyze" red,
  `Build` check red, "Merging is blocked" on the PR.
- Expected after the fix: a second commit with 8 unit tests, 45 tests in total, coverage on
  new code close to 100%, gate passed, merge allowed after 2 approvals.
- Why this failure and not a failing test: a failing test stops Maven before the scanner
  runs, and the deliverable requires a completed Sonar analysis.
- The prediction was written in the PR description before the first run finished; the PR
  keeps it under "Prediction".

## Observación

| Run                                                                                                     | Event        | Branch commit                                                        | Analysed revision                             | Step that failed             | Gate                                                                                |
| :------------------------------------------------------------------------------------------------------ | :----------- | :------------------------------------------------------------------- | :-------------------------------------------- | :--------------------------- | :---------------------------------------------------------------------------------- |
| [35143653182](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/35143653182) (run 34) | pull_request | `dd21082` `feat(service): add CotizacionCalculator for event quotes` | `307455b`, temporary merge commit             | 6, "Build, test and analyze" | FAILED: Coverage on New Code below 50% (0.0% on the 26 new lines to cover)          |
| [35145512732](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/35145512732) (run 35) | pull_request | `83f0689` `test(service): cover CotizacionCalculator`                | another temporary merge commit (checkout log) | none                         | PASSED, coverage on new code 100% according to JaCoCo (25/25 lines, 18/18 branches) |
| [35146688628](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/35146688628) (run 36) | push         | `72dc3e4`, squash of PR #11                                          | `72dc3e4`                                     | none                         | PASSED                                                                              |

- In run 34 the Maven build and the 37 tests passed, the analysis was published and the log
  ends with `QUALITY GATE STATUS: FAILED` followed by the coverage condition
  (`capturas/u1-run-red-step.png`). The PR page showed "Merging is blocked" with the `Build`
  check red (`capturas/u1-pr-blocked.png`).
- Sonar, PR analysis: http://66.70.181.143:26665/dashboard?id=gestor-salon-eventos-backend&pullRequest=11
  (shows the latest analysis, the passed one; the failed one is in
  `capturas/u1-sonar-pr-red.png`, the passed one in `capturas/u1-sonar-pr-green.png`).
- Finding observed: the only failed condition was Coverage on New Code. No new bug,
  vulnerability, hotspot or duplication was reported on the new class.
- Screenshots: `capturas/u1-run-red-step.png`, `capturas/u1-pr-blocked.png`,
  `capturas/u1-sonar-pr-red.png`, `capturas/u1-run-green.png`, `capturas/u1-sonar-pr-green.png`.

### Three kinds of red runs in this repository

| Kind                  | Example                                                                                                                                                                                                                                                                                                                                                                      | How to tell                                                            |
| :-------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------- |
| Configuration error   | [run 34570148134](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34570148134) (11 Sept, `historial`) failed at "Set up job", before any step ran                                                                                                                                                                                                        | No checkout, no Maven output                                           |
| Build or test failure | [run 34570293916](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34570293916) and nine other 11 Sept runs failed inside "Build and analyze" during the Maven build (compilation errors, then the startup test); the only green run of that day, `e4a54e1`, skipped the tests with `-DskipTests` instead of fixing them. The real fix is PR #4 in `main` | Maven `BUILD FAILURE` before any Sonar line                            |
| Quality gate failed   | [run 34783682667, attempt 1](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34783682667/attempts/1) (13 Sept, new-code definition) and run 34 of PR #11                                                                                                                                                                                                 | `QUALITY GATE STATUS: FAILED` in the log; the analysis exists in Sonar |

The 11 Sept runs belong to the exploratory history kept in the `historial` branch and are
listed only to show the difference; they are not part of this delivery.

## Corrección

- Change: commit `test(service): cover CotizacionCalculator` (`83f0689`) adds
  `CotizacionCalculatorTest` with 8 cases covering every branch. No production code changed.
- Technical reason: the gate measures coverage on new code; the only way to satisfy it without
  lowering the threshold or excluding the class is to test the class.
- Before/after: run 34 red (0.0% on 26 new lines) → run 35 green (100% on the same lines),
  same PR, same base commit `fe57fbe`. Merged as `72dc3e4` after approvals from Freddy and
  Chris; run 36 on `main` green.
- Earlier, real (not controlled) case: the first `main` analysis on 13 Sept failed the gate
  because "new code" was defined as the last 30 days and the whole project was new; the fix
  was a fixed baseline in SonarQube, not a commit, so the same commit passed on attempt 2.
  Recorded in ADR-0002. It is listed here as a configuration fix, not as the required
  commit-based correction.

## Decisión

- Gate condition that blocked: Coverage on New Code < 50%. Scope: **new code only**. On a PR,
  new code is the diff against `main`; on `main`, code changed since the baseline analysis of
  2026-09-13. Overall coverage is shown but is not a condition, so legacy code without tests
  does not block anyone.
- Decision for PR #11: **correct, then integrate**. Postponing was not an option because the
  class is needed and the fix is cheap; integrating with the gate red is impossible by ruleset
  and would not be accepted by the reviewers.
- What the gate does not prove and still needs human review: that the arithmetic matches the
  business rule (16% tax, 30% deposit, discounts up to 50%), that the PR description and risks
  are honest, that tests assert meaningful values and not just execute lines, and that Flyway
  migrations work on MySQL (tests run on H2 with Flyway off).

## Trazabilidad

For PR #11 three different hashes appear:

| Hash                 | What it is                                                                                                      | Where you see it                                                                                                     |
| :------------------- | :-------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------- |
| `dd21082`, `83f0689` | Commits on `feature/u1-controlled-failure`                                                                      | PR "Commits" tab, `git log` of the branch                                                                            |
| `307455b`            | Temporary merge of `dd21082` into `main`, created by GitHub for the `pull_request` event (`refs/pull/11/merge`) | Checkout log of run 34: "HEAD is now at 307455b Merge dd21082 into fe57fbe"; Sonar shows it as the analysed revision |
| `72dc3e4`            | Squash commit on `main` after merge                                                                             | `git log main`, run 36 of the `push` event                                                                           |

The run page lists the branch commit; the scanner analyses the merge commit, which is what
`main` would look like after merging; the merge commit is not in any branch and disappears
after the PR is merged. The Sonar analyses of 11 Sept still resolve because their commits
exist in `historial`; they are older analyses and are not mixed with this delivery.

## Participación individual

| Member               | Contribution to this deliverable                                                                                                      | YAML key or step they explain                                                                                                          | Live change they can make                                                                                |
| :------------------- | :------------------------------------------------------------------------------------------------------------------------------------ | :------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------- |
| José Luis Islas      | Rulesets (`main`, `historial`), Sonar access for the professor, secrets, screenshots of configuration, prompts file, review of PR #12 | `on:` with `push`/`pull_request` on `main`, and why the job name `Build` is the required check                                         | Add `workflow_dispatch` and explain what changes                                                         |
| Freddy Castro        | README section on CI and Sonar; review of PR #11                                                                                      | Step "Build, test and analyze": `SONAR_GOAL`, `sonar.qualitygate.wait`, PR mode vs branch mode, the `env` secrets; JaCoCo in `pom.xml` | Set `sonar.qualitygate.wait=false` on a branch and show that the check stays green while the gate is red |
| José Eduardo Aguilar | Author of PR #11 (controlled failure and its fix, both commits); review of PR #12                                                     | `actions/setup-java@v4` (`java-version`, `distribution`) and `mvn -B verify` (Surefire, tests)                                         | Change `java-version` to 21 and predict the outcome                                                      |
| Christopher Álvarez  | Author of this evidence report and PR #12 (screenshots, workflow comment); review of PR #11                                           | `actions/checkout@v4` with `fetch-depth: 0`, `actions/cache@v4` keys, `runs-on`                                                        | Remove `fetch-depth: 0` and explain the effect on new-code detection                                     |

## Servidor de SonarQube

Not part of the PDF: the professor asked for this by message on 11 September 2026, after the
team told him the analysis would run on a self-hosted server instead of SonarQube Cloud, which
the team had announced earlier. His answer was "Está perfecto, documéntenme nomás lo que
hicieron" (`capturas/u1-profesor-sonar-server.png`; the screenshot attached to that message
shows the first analysis on the new server, gate passed with 0.0% coverage).

1. **SonarQube Cloud, tried first.** The repository was bound to a SonarQube Cloud
   organization with automatic analysis, which needs no workflow file; no commit in the
   repository ever referenced `sonarcloud.io`. The trace that survives is the
   [`sonarqubecloud[bot]` comment on PR #1](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/1#issuecomment-5639060201)
   (11 Sept, project key `Skzyyx_gestor-salon-eventos-backend`): quality gate passed, 0 new
   issues, 0.0% coverage on new code. It was dropped because the free plan does not let the
   organization edit the quality gate, and the team needed its own coverage threshold on new
   code, the one later recorded in [ADR-0002](../adr/0002-sonarqube-quality-gate.md). The
   Cloud project is no longer available: the `sonarcloud.io` links inside that comment
   returned "not found" on 16 Sept.
2. **Self-hosted server.** The only server available to the team is reachable through a
   Pterodactyl panel, which cannot change kernel parameters on the host. The recent SonarQube
   versions tried first need them for the embedded Elasticsearch, so they did not start;
   SonarQube Server 9.9.8 (LTA 9.9) started and has run without problems since. Edition and
   branch plugin: see "Limitaciones e IA".
3. **First tests directly on `main`.** The first analyses on the new server were run from
   `main` to learn the tool. That work is archived in the `historial` branch and was redone
   through PRs during Sprint 1 ([Sprint 1 evidence](../evidence/sprint-01.md)).

## Limitaciones e IA

- SonarQube Server 9.9.8 Community Edition with the Community Branch Plugin installed
  (version shown in `capturas/u1-sonar-system_2.png`; edition and server version in
  `capturas/u1-sonar-system_1.png`). The Community Edition alone cannot analyse branches or
  pull requests; the plugin adds both, which is what makes `sonar.branch.name` and
  `sonar.pullrequest.*` work in the workflow. The plugin is community-maintained, not
  supported by SonarSource, so a server upgrade may break it; that risk is accepted for this
  course. Why this server and this version: "Servidor de SonarQube" above.
- PR decoration on GitHub (a Sonar check and a summary comment on each PR) is supported by
  the plugin but is not configured. It would need a GitHub App owned by the repository owner
  with permissions Checks: write, Pull requests: write and Metadata: read, installed on the
  repository; its App ID, client ID, client secret and private key entered in
  Administration → DevOps Platform Integrations → GitHub; and the project bound to
  `Skzyyx/gestor-salon-eventos-backend` under Project Settings → DevOps Platform Integration.
  Today the gate result reaches GitHub only through the `Build` check, which is the check
  required by the ruleset on `main`.
- The Sonar server requires a login. The professor has a read-only user sent separately;
  screenshots in `capturas/` back every Sonar link.
- Merge blocking is configured and demonstrated: `capturas/u1-pr-blocked.png` shows PR #11
  blocked by the required `Build` check.
- Pending, not claimed: migrations are not verified in CI (H2 with Flyway off); `service/`
  has tests only for `CotizacionCalculator`; the gate threshold stays at 50% until Sprint 3.
- AI use: Claude Code (Claude Fable 5.1) was used to analyse the git history against the
  working agreement, to draft the step-by-step plan, the controlled-failure class and its
  test, and this report, and to check the repository state through the GitHub API. Every
  file was compiled and run locally by its author before committing; the prompts are in
  [`prompts.md`](prompts.md). No secret was shared with the tool.
