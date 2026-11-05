# Lambda: imágenes

First phase of the image upload function in
[`docs/lambdas-plan.md`](../../docs/lambdas-plan.md), packaged as a container image. It
receives an image through its Function URL and checks it with the backend's rule: not empty,
at most 4 MB and only JPEG, PNG or WEBP, by its content. The token check and the upload to
Cloudinary come in week 9, in this same function.

The rule is `ImageValidator`, an identical copy of the backend's
`src/main/java/mx/gestorsalon/service/ImageValidator.java`. To change it, change the
backend's class and its tests first, then copy the file here: the pipeline fails if the two
files differ.

## Contract

`POST <function URL>` with the image bytes as the body (no multipart) and its type in
`Content-Type`, for example `image/png`. The type is checked by the content, not by the
header or the file name.

| Status | Body | When |
| :--- | :--- | :--- |
| 200 | `{"tipo":"image/png","bytes":70,"version":"a7c91e2"}` | A JPEG, PNG or WEBP of up to 4 MB |
| 400 | `{"message":"El archivo está vacío","version":"a7c91e2"}` | No body |
| 400 | `{"message":"Solo se permiten imágenes JPEG, PNG o WEBP. Tipo detectado: application/pdf","version":"a7c91e2"}` | Anything else, by its content |
| 413 | `{"message":"La imagen pesa más de 4 MB","version":"a7c91e2"}` | More than 4 MB |
| 405 | `{"message":"Método no permitido. Usa POST para revisar una imagen o GET para ver la versión","version":"a7c91e2"}` | Any method other than GET or POST; the answer brings `allow: GET, POST` |

The messages are the backend's. `version` is the short SHA of the commit the image was
built from (`local` for a local build). `GET <function URL>` answers how to use it, with the
version. In week 9 the 200 becomes `{ "url": ... }` and a missing or invalid token answers
401, as the plan says.

A direct invocation (`aws lambda invoke`, the console's Test tab) uses the shape a Function
URL sends: `{"isBase64Encoded": true, "body": "<the image in base64>"}`. There an answer
other than 200 comes as `{"statusCode": ..., "headers": ..., "body": "..."}`, the shape a
Function URL turns into an HTTP response.

## Run it locally

Tests, with Java 17 and Maven like the backend:

```bash
cd lambdas/imagenes
mvn -B test
cd ../..
```

The image, with Docker Desktop running. The Lambda base image brings an emulator, so the
container answers like the real function:

```bash
docker build -t imagenes:local lambdas/imagenes
docker run --rm -p 9000:8080 imagenes:local
```

In another terminal, with a 1x1 PNG:

```bash
PNG=iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==
curl -s "http://localhost:9000/2015-03-31/functions/function/invocations" \
  -d "{\"isBase64Encoded\":true,\"body\":\"$PNG\"}"
```

## Deployment

[`.github/workflows/lambda-imagenes.yml`](../../.github/workflows/lambda-imagenes.yml) runs
when this folder, the backend's `ImageValidator` or the workflow change.

- **Pull request** (`Build imagenes`): checks that both copies of `ImageValidator` are
  identical and builds the image; the tests run inside the build.
- **Push to `main`** (`Deploy imagenes`, in the `aws` environment): the same check, then it
  signs in to AWS, builds the image with `APP_VERSION` set to the short commit SHA, pushes it
  to ECR with that tag, points the function at it and waits. The smoke test sends a 1x1 PNG
  directly and through the Function URL (`image/png`, 70 bytes and this commit's version),
  and a text body that must answer 400. If the function does not exist (only the very first
  time), the run pushes the image, leaves a warning and stops.

| | |
| :--- | :--- |
| Region | `us-east-2` |
| ECR repository | `gestor-salon-imagenes`: keeps the last 3 images, scans every push |
| Function | `gestor-salon-imagenes`: x86_64, 512 MB, 10 s timeout, public Function URL |
| Execution role | `gestor-salon-imagenes-exec`: CloudWatch Logs only; logs kept 14 days |
| Deploy credentials | IAM user `gha-deploy-gestor-salon`, keys in the `aws` environment ([ADR-0005](../../docs/adr/0005-deploy-credentials-without-oidc.md)) |
| Environment variable | `IMAGENES_URL`: the Function URL, for the smoke test |
