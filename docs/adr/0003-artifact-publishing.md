\# ADR-0003: Version, artifact and Docker image published to Nexus



\## Context



Avance 3 asks the team to publish the project's artifact to its Nexus with `mvn deploy`,

to build a multi-stage Docker image tagged with a version and push it to the Docker

registry in Nexus, to check that the container runs from that registry, and to add a step

to `ci.yml` that builds the image with the commit SHA as its tag. Nexus credentials may

not be committed: they live only in each member's local `settings.xml`. The deliverable is

a pull request that is still open at the deadline.



The team's Nexus Repository runs on the same Pterodactyl panel as SonarQube, reachable at

`http://<nexus-host>:<port>`. The working agreement says that releases are always cut from

`main`.



\## Decision



\- \*\*Version: `0.1.0-SNAPSHOT`, on purpose.\*\* The work is published from

&#x20; `feature/avance-3-artefactos-docker`, whose pull request is not merged before the

&#x20; deadline. A RELEASE (`0.1.0`) is immutable and, by the working agreement, only comes from

&#x20; `main`; a SNAPSHOT can be republished while the work changes. The first RELEASE will be

&#x20; cut from `main` after the merge.

\- \*\*Artifact:\*\* `mvn deploy` from a member's laptop publishes to `maven-snapshots`.

&#x20; `<distributionManagement>` in `pom.xml` points to `maven-snapshots` and `maven-releases`

&#x20; with the id `nexus`; each member's `\~/.m2/settings.xml` holds a `<server>` and a

&#x20; `<mirror>` with that same id. `application-local.yml` and `application-dev.yml` are

&#x20; excluded from the jar so local secrets never reach Nexus.

\- \*\*Image:\*\* multi-stage `Dockerfile` (Maven + JDK 17 to build, JRE 17 on Alpine to run, as

&#x20; a non-root user). Tagged with the project version, `0.1.0-SNAPSHOT`, never only `latest`,

&#x20; and pushed to `docker-hosted` from a laptop after `docker login`.

\- \*\*CI:\*\* `ci.yml` builds the image on every run with the commit SHA as its tag

&#x20; (`gestor-salon-eventos:<sha>`), so every image points to exactly one commit. CI does not

&#x20; push: it has no Nexus credentials.

\- \*\*Nexus:\*\* `maven-releases` (hosted, Disable redeploy), `maven-snapshots` (hosted, Allow

&#x20; redeploy), `maven-central` (proxy of Maven Central), `maven-public` (group of the three,

&#x20; the only URL laptops use), `docker-hosted` (hosted, its own HTTP port, Allow redeploy

&#x20; because it holds SNAPSHOT images). Anonymous access is off; team members have a role

&#x20; limited to those repositories.



\## Alternatives considered



\- \*\*Publish from CI on every merge to `main`.\*\* Rejected for this deliverable: the pull

&#x20; request stays open, so it would never run before the deadline, and it would need Nexus

&#x20; credentials stored as GitHub secrets. Revisit once releases are cut from `main`.

\- \*\*Publish `0.1.0` now.\*\* Rejected: a RELEASE from a feature branch breaks the working

&#x20; agreement, and a fix would need a new version number.

\- \*\*Nexus on each laptop (`localhost`).\*\* Rejected: each member would see a different

&#x20; repository and nothing published by one would reach the others. The shared server is the

&#x20; same choice the team made for SonarQube.



\## Trade-offs / consequences



\- Nexus speaks plain HTTP, so Docker lists the registry under `insecure-registries` and

&#x20; credentials travel unencrypted. Mitigated with personal users that only reach these

&#x20; repositories; HTTPS behind a reverse proxy is pending.

\- Publishing depends on a member running `mvn deploy` and `docker push`; nothing publishes

&#x20; automatically.

\- The `0.1.0-SNAPSHOT` image tag is overwritten by the next push; the SHA-tagged images

&#x20; built in CI are the ones that trace back to a commit.



\## Validation evidence



\- Pull request of Avance 3: \[#21](https://github.com/Skzyyx/gestor-salon-eventos-backend/pull/21),

&#x20; with the three screenshots in its description: the artifact in `maven-snapshots`, the

&#x20; image and its tag in `docker-hosted`, and `docker images` with the size.

\- CI run that builds `gestor-salon-eventos:<sha>`: \ https://github.com/Skzyyx/gestor-salon-eventos-backend/actions/runs/38685972298
