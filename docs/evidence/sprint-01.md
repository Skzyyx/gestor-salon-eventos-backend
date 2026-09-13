# Sprint 1 Evidence

## Sprint Goal
Adopt the GitHub Flow described in the working agreement for real: `main` protected by a
ruleset, every change through a Pull Request with 2 approvals, continuous integration running
the test suite on every PR, and a blocking SonarQube quality gate. Redo, under that flow, the
exploratory CI and SonarQube work of September 11, which had been pushed directly to `main`.

## Incremento demostrable
- `main` contains the 2 baseline commits plus one squash commit per PR (8 once this PR is
  merged), all with Conventional Commits prefixes and no merge commits.
- `Build` pipeline (GitHub Actions) on every PR and every push to `main`: compiles, runs the
  test suite (37 tests: 36 mapper tests plus `contextLoads`) and runs the SonarQube analysis
  with JaCoCo coverage. It is a required status check.
- Blocking quality gate `gestor-salon-sprint1` (`sonar.qualitygate.wait=true`), documented in
  ADR-0002.
- Mandatory PR template in `.github/pull_request_template.md` and a "Pull Requests" section in
  the working agreement.
- `historial` branch kept as a read-only archive of the exploratory work; the Actions runs and
  Sonar analyses of September 11 still point to existing commits.

## Evidencia aplicable

### Pull Requests
| PR | Title | Author | Approved by | Merged |
| :--- | :--- | :--- | :--- | :--- |
| [#3](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/3) | docs(wa): mandatory PR format, quality gates and historial branch rule | José Luis | Freddy, José Eduardo | 2026-09-12 |
| [#4](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/4) | fix: make main compile and pass mvn clean test | Chris | Freddy, José Luis | 2026-09-12 |
| [#5](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/5) | ci: add GitHub Actions workflow with Maven build and tests | José Luis | Freddy, José Eduardo | 2026-09-12 |
| [#6](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/6) | refactor(mapper): standardize mappers as Spring components | Freddy | Chris, José Luis | 2026-09-12 |
| [#7](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/7) | test(mapper): add unit tests for all mappers | José Eduardo | Freddy, José Luis | 2026-09-12 |
| [#8](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/8) | ci: SonarQube analysis with JaCoCo coverage and PR decoration | Freddy | Chris, José Eduardo | 2026-09-13 |
| [#9](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/9) | refactor: fix Sonar findings on covered code and test setup | Chris | José Eduardo, José Luis | 2026-09-13 |
| [#10](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/10) | docs(evidence): Sprint 1 evidence and ADR validation | José Eduardo (commits from all four) | José Luis, Chris | this PR |

### Branch protection
- Ruleset on `main`: pull request required with 2 approvals, squash as the only merge method,
  `Build` status check required, force pushes blocked, branch deletion blocked.
- _(Screenshot line added by José Luis in his turn.)_

### Pipeline
- First `Build` run on a PR (#5): [run 34720451041](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34720451041).
- First run with the mapper tests, 37 tests (#7): [run 34722331601](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34722331601).
- First run with the SonarQube step (#8): [run 34783530940](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34783530940).
- `main` run after merging #8, commit `5ae782b`: [attempt 1](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34783682667/attempts/1)
  failed the quality gate; [attempt 2](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34783682667)
  passed with the same commit after the new-code baseline was fixed in SonarQube.
- `main` run after merging #9: [run 34785880514](https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/34785880514).

### Quality gate
- Gate `gestor-salon-sprint1` assigned to the project: `img/adr-0002-quality-gate.png`.
- PR #8 analysis, gate green with coverage above 0%: [Sonar, PR 8](http://66.70.181.143:26665/dashboard?id=gestor-salon-eventos-backend&pullRequest=8).
- PR #9 analysis, first PR with Java changes evaluated under the gate, gate green: [Sonar, PR 9](http://66.70.181.143:26665/dashboard?id=gestor-salon-eventos-backend&pullRequest=9).
- `main` branch: [Sonar, main](http://66.70.181.143:26665/dashboard?id=gestor-salon-eventos-backend).

### Deployment / infrastructure
N/A — todavía no corresponde a este Sprint.

## Diagnóstico, decisión y trade-off
_(Turns 2, 3 and 4: Chris, Freddy and José Luis each add one block here.)_

## Contribuciones del equipo
_(Each member adds their own entry in their turn.)_

## Mini Definition of Done
_(Turn 4: José Luis.)_

## Retro: Keep / Change / Next experiment
_(Turn 4: José Luis.)_

## Uso de IA
_(Turn 5: José Eduardo.)_