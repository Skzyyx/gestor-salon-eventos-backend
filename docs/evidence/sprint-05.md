# Sprint 5 Evidence

> Completar con la plantilla vigente. Conserva este archivo; no sobrescribas evidencias de otros Sprints.

## Sprint Goal

## Incremento demostrable

## Evidencia aplicable

## Diagnóstico, decisión y trade-off

## Contribuciones del equipo

- **José Eduardo Aguilar:** slug defect at business creation found by `AdminServiceTest`
  and fixed with `SlugUtils` (PR #16, red then green); the same defect when editing the
  profile, red then green in the Avance 2 PR (#20), which he opened. Reviewed: #15, #17, #19.

- **Christopher Álvarez:** `TurnoServiceTest` and `ServicioServiceTest` (PR #17);
  `PaqueteServiceTest` in the Avance 2 PR (#20), which he also reviews. Reviewed: #16, #18.

- **Freddy Castro:** `RolNegocioServiceTest` and `ImageUploadServiceTest` (PR #18);
  `EquipoServiceTest` in the Avance 2 PR (#20), which he also reviews. Reviewed: #15, #17, #19.

- **José Luis Islas:** test pipeline: separate test and analysis steps, test summary,
  test reports artifact, `failIfNoTests` and the JDK check (PR #15); `ci.yml` on every push
  (PR #19); `JwtServiceTest` in the Avance 2 PR (#20). Reviewed: #16, #18.

### Coverage round (review of PR #20)

- **Jose Aguilar:** `ClienteServiceTest` and `CategoriaServiceTest` (services that had
  no tests), `DayOfWeekConverterTest`, and more cases in `ServicioServiceTest`,
  `TurnoServiceTest` and `PaqueteServiceTest`; the 80% coverage gate in `pom.xml` and
  `ci.yml`, with a coverage summary on every run. Coverage after his turn: 81.6% of lines,
  84.8% of branches.

- **Christopher Álvarez:** `@WebMvcTest` tests with the real security configuration for the
  catalog, auth, admin, business and image endpoints, and `GlobalExceptionHandlerTest`; they
  found that a missing permission answered 500 instead of 403, fixed in
  `GlobalExceptionHandler` (red then green); more cases in `NegocioServiceTest` and
  `AdminServiceTest`. Coverage after his turn: 90.3% of lines, 91.1% of branches.

- **Freddy Castro:** `JwtAuthenticationFilterTest`, `CustomUserDetailsTest`,
  `CustomUserDetailsServiceTest`, `SecurityUtilsTest`, `AuthServiceTest` and
  `EquipoControllerTest` (team and role endpoints with real authorities); more cases in
  `EquipoServiceTest` and `RolNegocioServiceTest`. Final suite: 454 tests, 99.5% of lines,
  98.2% of branches.

## Mini Definition of Done

## Retro: Keep / Change / Next experiment

## Uso de IA
