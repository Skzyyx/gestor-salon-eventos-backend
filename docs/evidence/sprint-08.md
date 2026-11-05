# Sprint 8 Evidence

> Completar con la plantilla vigente. Conserva este archivo; no sobrescribas evidencias de otros Sprints.

## Sprint Goal

## Incremento demostrable

## Evidencia aplicable

## Diagnóstico, decisión y trade-off

## Contribuciones del equipo

- **José Luis Islas:** pending.

- **José Eduardo Aguilar:** the deploy workflow (`lambda-imagenes.yml`: the image built on pull
  requests; ECR, update and smoke tests from `main`), in #30.

- **Christopher Álvarez:** the first phase of the image function (`lambdas/imagenes/`): a
  handler over an identical copy of `ImageValidator`, 12 tests and its container image on the
  Java 17 Lambda base image, checked with the local emulator, in #29.

- **Freddy Castro:** OIDC tried and blocked by the AWS-managed SCP; IAM user
  `gha-deploy-gestor-salon` with the minimum deploy permissions, checked with the policy
  simulator; ADR-0005, the image function in the Lambda plan and the README section, in #31;
  405 for methods other than GET and POST, test first, deployed by the pipeline with no
  manual step, in #32.

## Mini Definition of Done

## Retro: Keep / Change / Next experiment

## Uso de IA
