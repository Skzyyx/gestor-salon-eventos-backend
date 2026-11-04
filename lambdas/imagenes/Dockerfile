# Image function as an AWS Lambda container image. Build it from the repository root:
#   docker build -t imagenes:local lambdas/imagenes

# ---- Stage 1: compile and test ----
# Unlike the backend's Dockerfile, the tests run here: this is the only place they run
# before a deploy, so if one fails there is no image.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# pom.xml first: while the dependencies do not change, Docker reuses this layer.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# target/classes holds the code; target/dependency, the jars it needs at runtime
# (aws-lambda-java-core, and tika-core with its own dependencies).
RUN mvn -B package dependency:copy-dependencies -DincludeScope=runtime

# ---- Stage 2: the official Lambda runtime for Java 17 (Amazon Linux 2023) ----
# It brings the Lambda runtime and a local emulator. Maven and the source code stay in
# stage 1.
FROM public.ecr.aws/lambda/java:17.al2023

# The pipeline passes the short commit SHA; a local build answers "local".
ARG APP_VERSION=local
ENV APP_VERSION=${APP_VERSION}

COPY --from=build /src/target/classes ${LAMBDA_TASK_ROOT}
COPY --from=build /src/target/dependency/ ${LAMBDA_TASK_ROOT}/lib/

# The handler is set here, not in the Lambda console.
CMD [ "mx.gestorsalon.lambda.ImagenesHandler::handleRequest" ]