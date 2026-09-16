# AI prompts used for the Unit 1 work

Tool: Claude Code, model Claude Fable 5.1, run from the repository folder. Prompts are quoted
verbatim when we still have them; otherwise the entry says "summary". No token, password or
personal data was ever included in a prompt.

| Date | Purpose | Prompt | What we changed or verified afterwards |
| :--- | :--- | :--- | :--- |
| 2026-09-12 | Analyse the exploratory history against the working agreement and plan the re-execution as PRs | summary: asked for a comparison of the 11 Sept commits on `main` with the working agreement and for a playbook to redo the work as PRs with an author and two reviewers each | The plan's reviewer assignments did not match the real approvers; the evidence was rewritten from the repository state |
| 2026-09-13 | Rewrite the evidence step of the playbook against the real repo state | summary: asked to redo the evidence section of the playbook using only files, branches, approvers and runs that actually existed | Two screenshots the plan cited had never been taken; they were dropped |
| 2026-09-16 | Analyse the full history and compare with the deliverable | "Analiza el historial de todo el repositorio. Desde los primeros commits que no seguian el Working Agreement, como hicimos la rama historial, la protegimos y después recreamos todo de la manera correcta. Después, analiza lo que pide el documento adjunto como entregables y compara que es lo que nos falta. Probablemente sean muchos puntos pero fáciles de resolver." | Found that the `historial` ruleset had no target and that no run in the delivery had a commit-based fix; both confirmed by hand in GitHub |
| 2026-09-16 | Turn the gap list into a step-by-step plan | "Arma un plan para ir avanzando paso a paso con lo que falta. Debe estar detallado y no esperar que el usuario adivine que debe hacer." | The proposed class and test were compiled and run locally with JDK 17 before being adopted (45 tests, 26 lines to cover) |
