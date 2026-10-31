\# AWS Lambda plan



Status: proposal; the professor's answers are at the end. Repository layout:

\[ADR-0004](adr/0004-frontend-in-this-repository.md).



\## Premise



The system already runs, or has a place to run, outside AWS: the Spring Boot backend, MySQL on

Aiven and the images on Cloudinary stay as they are. AWS Lambda is used only where it adds

something the server does not give, and the system keeps working without these functions.

Nothing is created in AWS before the professor assigns the week 8 work.



\## Summary



| | Health monitor | Image upload | Orphan image cleanup |

| :--- | :--- | :--- | :--- |

| Course week | 8: Lambda without containers | 9: Lambda with container images | Optional, at the end |

| Package | ZIP, runtime `java17.al2023` | Image based on `public.ecr.aws/lambda/java:17.al2023`, stored in ECR | ZIP, `java17.al2023` |

| Trigger | EventBridge Scheduler, every 30 minutes | Function URL, called by the frontend | EventBridge Scheduler, weekly |

| Code | `lambdas/monitor-salud/` | `lambdas/imagenes/` | `lambdas/limpieza-imagenes/` |

| Talks to | Aiven API, MySQL (read only) | Cloudinary | MySQL (read only), Cloudinary |

| Secrets (SSM SecureString) | Aiven token, `monitor\_lectura` password | JWT secret, Cloudinary credentials | `monitor\_lectura` password, Cloudinary credentials |



Each function has its own `pom.xml`; none is a module of the backend's `pom.xml`. All of them

run outside a VPC and reach Aiven, the Aiven API and Cloudinary over the internet with TLS, so

no NAT gateway is needed (one would cost about 33 USD a month).



The AWS account uses the new AWS experience: the project lives in `us-east-2` (Ohio) and its

Region cannot be changed, every team member is an administrator of the project, and IAM role

manager attaches the broad `PowerUserRole` to new functions by default. Each function gets its

own execution role instead, with only what it needs (its SSM parameters and its logs).



\## Health monitor (week 8)



Why Lambda: a scheduled job that must not depend on the backend being up. It runs a few

seconds every 30 minutes and does not exist the rest of the time.



Every run:



1\. `GET https://api.aiven.io/v1/project/<project>/service/<service>` with

&#x20;  `Authorization: aivenv1 <token>`, and reads `service.state`.

2\. If the state is `POWEROFF`: `PUT` to the same URL with `{"powered": true}`, and logs that

&#x20;  it had to power the service on.

3\. If it is `RUNNING`: connects as `monitor\_lectura` and runs a real query on the tables, for

&#x20;  example `SELECT COUNT(\*) FROM negocios`.

4\. Any failure ends the run with an error. A CloudWatch alarm on the function's `Errors`

&#x20;  metric emails the team through SNS.



What Aiven guarantees: a Free plan service can be powered off when it has no "continuative

activity", and Aiven does not define what counts. The periodic query is an attempt; powering

it back on is the safety net; only a paid plan (Developer, about 5 USD a month) is never

powered off. Powering on restores the latest backup and replays the binlog, which takes minutes

to hours. A service that stays powered off for more than 180 days is deleted.



The Aiven token is a personal token with an expiry date; its renewal goes on the team's

calendar. A 401 or 403 from the API fails the run, so the alarm also catches an expired token.



\## Image upload (week 9)



Why Lambda: it takes user-uploaded files out of the server. An untrusted file is parsed in an

isolated environment that is thrown away, the server stops holding images in memory, and the

function scales by itself when many users upload at once. It reuses `ImageValidator`, which

already has tests in the backend.



\### Contract



Request:



\- `POST <function URL>`

\- Body: the image bytes as they are (no multipart).

\- `Content-Type`: the image type, for example `image/png`.

\- `Authorization: Bearer <token>`: the JWT the backend returns from `POST /api/v1/auth/login`.



Responses, with the same `{ "message": ... }` body as the backend's `GlobalExceptionHandler`:



| Status | Body | When |

| :--- | :--- | :--- |

| 200 | `{ "url": "https://res.cloudinary.com/..." }` | Uploaded |

| 400 | `{ "message": "El archivo está vacío" }` or `{ "message": "Solo se permiten imágenes JPEG, PNG o WEBP. Tipo detectado: ..." }` | Empty, or not JPEG, PNG or WEBP by its content |

| 401 | `{ "message": "..." }` | Missing, invalid or expired token |

| 413 | `{ "message": "La imagen pesa más de 4 MB" }` | More than 4 MB |

| 500 | `{ "message": "Error al procesar la imagen" }` | Cloudinary failed |



\### Decisions



\- \*\*4 MB everywhere.\*\* A Function URL accepts 6 MB per synchronous request, and binary bodies

&#x20; arrive base64-encoded, about 4/3 larger: 4 MB of image is about 5.4 MB of payload. The

&#x20; backend (`spring.servlet.multipart.max-file-size`), the frontend (`MAX\_IMAGE\_BYTES`) and

&#x20; `ImageValidator.MAX\_BYTES` use the same limit.

\- \*\*Token checked inside the function.\*\* The backend signs its JWTs with HS256 and

&#x20; `JWT\_SECRET`. The Lambda reads the same secret from SSM and checks the signature and the

&#x20; expiry. API Gateway's JWT authorizer cannot verify HS256 tokens, and with a single exposed

&#x20; function a separate authorizer adds nothing. Development and production use different

&#x20; secrets; the team shares the development one.

\- \*\*Accepted limitation:\*\* the Lambda does not query the database, so a user deactivated less

&#x20; than 24 hours ago (the token's lifetime) can still upload until the token expires.

\- \*\*Cloudinary:\*\* the same folder as today (`servicios/`); credentials in SSM.

\- \*\*CORS\*\* is set on the Function URL: the frontend's origin, `POST`, and the `authorization`

&#x20; and `content-type` headers. The backend's CORS (`APP\_CORS\_ALLOWED\_ORIGINS`) is separate.

\- \*\*Abuse brake:\*\* low reserved concurrency (for example 5), if the account's concurrency

&#x20; quota allows it (new accounts may start at 10).

\- \*\*Switch in the frontend:\*\* `VITE\_UPLOAD\_URL`. Empty means the backend's `/images/upload`,

&#x20; as before.



\## Orphan image cleanup (optional)



Images are left unused when one is replaced or when saving fails after the upload, and they

take space in Cloudinary's free plan. Once a week the function lists the assets under

`servicios/`, compares them with the image URLs stored in `servicios`, `paquetes` and

`negocios`, active or not (a soft-deleted row can be reactivated), and deletes only the assets

that no row mentions and that are older than 7 days. The first weeks it only reports, without

deleting. It fits with Terraform (week 11) and CloudWatch (week 15).



\## Costs



\- Lambda, EventBridge Scheduler, CloudWatch (logs and a few alarms) and SNS email stay within

&#x20; the always-free allowances at this volume.

\- ECR: a few hundred MB of images, with a lifecycle policy that keeps the last 3. Logs are

&#x20; kept 14 days.

\- The AWS account is on the Free plan: its credits cover the rest, and a monthly budget of

&#x20; 5 USD alerts at 50, 80 and 100%.



\## Discarded options



| Option | Why not |

| :--- | :--- |

| Whole backend in Lambda | The system already has a place to run. It would fit (the image is about 400 MB of a 10 GB limit), but every Spring Boot cold start takes seconds and it would be another copy of the system to operate |

| Separate JWT authorizer | With one exposed function, checking the token inside it is simpler; API Gateway's JWT authorizer does not support HS256 |

| Quote calculation | Microseconds inside the backend; in Lambda it only adds latency and a deployment, and the endpoint does not exist |

| Notifications, reports, exports | They do not exist in the system: new features, not a migration |

| RDS, VPC with NAT, ECS, Cognito | They cost by the hour or replace something that works, and no deliverable asks for them |



\## Risks



\- Request payload of 6 MB: covered by the 4 MB limit everywhere.

\- Cold start of the Java image function on the first upload after a while (a few seconds):

&#x20; acceptable for an upload.

\- The Free plan has an end date and limited credits; the owner sees both in AWS Settings →

&#x20; Billing, and the end date is on the team's calendar. Nothing is charged unless the account is

&#x20; upgraded to the paid plan.

\- Aiven can still power the database off; the monitor powers it back on, but the restore takes

&#x20; minutes to hours.



\## Professor's answers



\- Accepts the proposal: pending.

\- Is the repository layout graded: pending.

\- Delivery date and format: pending.

