# Sprint 8 Evidence

> Completar con la plantilla vigente. Conserva este archivo; no sobrescribas evidencias de otros Sprints.

## Sprint Goal

Deliver the week 7 practice on the project: the first phase of the image function, packaged
as a container image and deployed by GitHub Actions on every push to `main`.

## Incremento demostrable

`gestor-salon-imagenes` answers at its Function URL: a POST with a JPEG, PNG or WEBP returns
the type detected in its content and the `version` of the commit that deployed it; a PDF
answers 400 and a file over 4 MB answers 413, with the backend's messages. URL:
`https://5u44xekiane4veqbuvobbtxti40xsanr.lambda-url.us-east-2.on.aws/`. The 405 change (#32) reached the function after its merge, with no one
opening the AWS console.

## Evidencia aplicable

- First run: image pushed to ECR, warning that the function did not exist yet: https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/41794155982/attempts/2
- The same run again after creating the function: deploy and smoke tests green: https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/41794155982/attempts/5
- The 405 change, deployed by itself: https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/41961544140
- ECR `gestor-salon-imagenes`: images tagged with the short SHAs of those commits; the
  lifecycle rule keeps 3.
- Pull requests: #29 (function and image), #30 (pipeline), #31 (ADR-0005 and plan),
  #32 (405).

## Diagnóstico, decisión y trade-off

The course signs in to AWS with OIDC, and the project's account denies creating identity
providers (AWS-managed SCP). Decision: an IAM user with the same minimum permissions and its
keys in a `main`-only environment. Trade-off: a key that does not expire
([ADR-0005](../adr/0005-deploy-credentials-without-oidc.md)).

## Contribuciones del equipo

- **José Luis Islas:** ECR repository with its 3-image rule, the `aws` environment with the
  deploy key, the function created from the first image with its own execution role and its
  Function URL, and this evidence, in #33.

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
