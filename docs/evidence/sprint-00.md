# Sprint 0 Evidence

> **Nota de migración:** este Sprint se realizó sobre el repositorio de práctica `orderflow-starter`
> (https://github.com/Skzyyx/orderflow-starter). A partir del Sprint 1, el proyecto del curso es
> `gestor-salon-eventos-backend` (https://github.com/Skzyyx/gestor-salon-eventos-backend), con autorización del profesor.
> La evidencia se conserva sin modificar como registro histórico del baseline.

## Sprint Goal
Establecer el baseline reproducible de OrderFlow: repositorio del equipo, build/tests verificados desde fresh clone, y ejecución local de la API.

## Repository baseline
- Repo: https://github.com/Skzyyx/orderflow-starter.git
- Initial commit: `0542e04` — "Initial Commit"
- Fresh clone verified by: José Eduardo Aguilar García, Freddy Alí Castro Román y Christopher Álvarez Centeno — clon limpio desde cero, cada uno en su propia máquina

## Baseline execution
- Tests: `mvn clean test` → BUILD SUCCESS (3 tests: 2 en `orders-api` — `OrderServiceTest`, 1 en `notifications-lambda` — `NotificationServiceTest`)
- Artifact: `mvn package` → BUILD SUCCESS. Jars generados:
  - `orders-api/target/orders-api-0.1.0-SNAPSHOT.jar`
  - `notifications-lambda/target/notifications-lambda-0.1.0-SNAPSHOT.jar`
- Endpoint:
  - `GET /actuator/health` → `{"status":"UP"}`
  - `POST /api/orders` (`{"customerId":"team-demo","total":150.00}`) → `{"id":1001,"customerId":"team-demo","total":150.00,"status":"CREATED"}`
  - `GET /api/orders` → `[{"id":1001,"customerId":"team-demo","total":150.00,"status":"CREATED"}]`

## Value Stream
- AS-IS: `value-stream/as-is.md`
- TO-BE: `value-stream/to-be.md`

## Incremento demostrable
Baseline de OrderFlow corriendo end-to-end de forma reproducible: desde un fresh clone del repositorio hasta la ejecución local del servicio y la validación funcional de los tres endpoints (`/actuator/health`, `POST /api/orders`, `GET /api/orders`), verificado de forma independiente por tres integrantes del equipo. No se agregó funcionalidad de negocio nueva, ya que no correspondía a este Sprint.

## PR / pipeline / deployment / infraestructura
N/A — todavía no corresponde a este Sprint.

## Decisión y trade-off
Se decidió estandarizar el trabajo del equipo en PowerShell 7 (`pwsh`) en lugar de `cmd.exe`, no solo porque la fricción observada en el AS-IS lo evidenció (`Invoke-RestMethod` falló en `cmd.exe`), sino porque PowerShell 7 es multiplataforma y es el shell por defecto de los runners de GitHub Actions (`shell: pwsh`), lo que deja el terreno listo para el pipeline de CI de la Semana 3.

**Trade-off**: esto implica que todo el equipo debe instalar y usar PowerShell 7 de forma consistente (no todos lo tenían inicialmente), pero a cambio se elimina la variabilidad de entorno que causó la fricción original, y se anticipa el entorno que usará el pipeline automatizado más adelante.

## Demo mínima reproducible
```bash
git clone https://github.com/Skzyyx/orderflow-starter.git
cd orderflow-starter
mvn clean test
mvn package
mvn -pl orders-api spring-boot:run
```
Luego, en otra terminal (PowerShell 7):
```powershell
curl http://localhost:8080/actuator/health
Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -ContentType "application/json" -Body '{"customerId":"team-demo","total":150.00}'
curl http://localhost:8080/api/orders
```

## Contribuciones del equipo
- José Luis Islas Molina (Líder) — Creación del repositorio del equipo, importación del starter y publicación del commit baseline "Initial Commit" (`0542e04`) en `main`.
- Christopher Álvarez Centeno — Verificación independiente del fresh clone y del baseline (tests, package, ejecución de endpoints).
- Freddy Alí Castro Román — Verificación independiente del fresh clone y del baseline (tests, package, ejecución de endpoints).
- José Eduardo Aguilar García — Fresh clone y verificación de reproducibilidad del baseline (tests, package, ejecución de endpoints), documentación del AS-IS, TO-BE y evidencia del Sprint 0.

## Mini Definition of Done
- [x] Repo y commit baseline identificables
- [x] Fresh clone verificado
- [x] Build/tests, artifact y endpoint reproducibles
- [x] AS-IS, TO-BE y Working Agreement completos
- [x] Decisión/trade-off defendible

## Retro: Keep / Change / Next experiment
- **Keep**: La división clara de responsabilidades (bootstrap vs. verificación) funcionó sin fricciones de comunicación entre el líder y el resto del equipo.
- **Change**: Estandarizar el entorno de trabajo (PowerShell 7, Maven Wrapper) desde el inicio del próximo sprint, para no descubrir fricciones de entorno a mitad del proceso.
- **Next experiment**: Implementar el Maven Wrapper (`mvnw`) y un script `check-env.ps1` que verifique automáticamente Java, Git y la versión del wrapper en cada máquina del equipo.

## Uso de IA
- Herramienta: Claude
- Prompt relevante: Ayuda para entender el README del proyecto, ejecutar el flujo de fresh clone/build/test/package, y estructurar la evidencia del Sprint 0, AS-IS y TO-BE
- Qué verificamos/cambiamos: Se verificó manualmente cada resultado en terminal (clone, tests, package, endpoints) antes de documentarlo; la IA no generó código ni resultados, solo guió el proceso y ayudó a detectar inconsistencias entre el AS-IS y el TO-BE