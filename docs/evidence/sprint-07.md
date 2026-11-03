# Sprint 7 Evidence

> Completar con la plantilla vigente. Conserva este archivo; no sobrescribas evidencias de otros Sprints.

## Sprint Goal

## Incremento demostrable

## Evidencia aplicable

## Diagnóstico, decisión y trade-off

## Contribuciones del equipo

- **José Luis Islas:** selective Lambda proposal sent to the professor; frontend imported into
  `frontend/` with ADR-0004, in #[22]; AWS account prepared (budget, read-only IAM group,
  quotas checked) with no resources created.

- **José Eduardo Aguilar:** frontend workflow (lint, typecheck, build) with pnpm pinned and the
  worker modal's type error fixed, in #26; uploads to a configurable destination
  (`VITE_UPLOAD_URL`) with a 4 MB check in the browser, in #28.

- **Christopher Álvarez:** `ImageValidator`, the upload validation without Spring that the image
  Lambda will reuse, with the 4 MB limit and the 413 answer, in #23; CORS origins from
  `APP_CORS_ALLOWED_ORIGINS` and the missing `PATCH`, in #27.

- **Freddy Castro:** superadmin password changed on Aiven and removed from the README, and a
  read-only database user for the monitor, in #24; Lambda plan with the image upload
  contract, in #25.

## Mini Definition of Done

## Retro: Keep / Change / Next experiment

## Uso de IA

