# ADR-0001: Estrategia de branching — GitHub Flow

## Contexto

El equipo necesita una estrategia de branching para el proyecto `gestor-salon-eventos-backend`
durante el curso de DevOps. Somos un equipo pequeño (4 integrantes), con entregas cortas por Sprint,
una sola versión del producto en desarrollo y la intención de integrar CI/CD progresivamente
(quality gates en Semana 3, CI en Semana 4, CD desde Semana 8).

## Decisión

Adoptar **GitHub Flow**: una sola rama de larga vida (`main`, protegida y siempre desplegable) y
ramas cortas `feature/*` por cada cambio, que se integran mediante Pull Request y se eliminan al hacer merge,
con al menos 2 aprobaciones y quality gates en verde. Las reglas concretas están en `working-agreement.md`.

## Alternativas consideradas

- **Git Flow** (`develop`, `release/*`, `hotfix/*`): más ramas y ceremonias de las que necesita un
  equipo de 4 con una sola versión activa; retrasa la integración y complica el CD continuo.
- **Trunk-Based Development** (commits directos a `main` o ramas de horas): integración más rápida,
  pero exige feature flags y una suite de pruebas muy madura que aún no tenemos; además elimina la
  revisión por PR que el curso pide como evidencia.

## Trade-offs / consecuencias

- (+) Flujo simple de aprender y de explicar; encaja directo con PRs, Code Review y CI/CD en GitHub.
- (+) `main` siempre desplegable simplifica el Continuous Delivery a partir del Sprint 3.
- (−) No hay rama de estabilización: un cambio defectuoso que llegue a `main` afecta de inmediato;
  se mitiga con la protección de rama, 2 revisores y quality gates obligatorios.
- (−) Requiere disciplina para mantener las ramas cortas y actualizadas con `main`.

## Evidencia de validación

- Regla de protección de `main` configurada en GitHub (PR obligatorio, 2 aprobaciones, sin push directo).
- Primeros PRs del Sprint 1 integrados siguiendo el flujo (ver `docs/evidence/sprint-01.md`).
