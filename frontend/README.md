# Frontend: Gestor de Salones de Eventos

React 19, Vite 8 and TypeScript, managed with pnpm. It talks to the backend at the root of
this repository. Why both live here: [ADR-0004](../docs/adr/0004-frontend-in-this-repository.md).

## Run it locally

Requirements: Node 24 and pnpm.

```bash
cd frontend
cp .env.example .env    # .env is git-ignored
pnpm install
pnpm dev                # http://localhost:5173
```

Start the backend first (`mvn spring-boot:run` from the repository root). It accepts browser
requests from `http://localhost:5173`.

## Checks

```bash
pnpm lint
pnpm typecheck
pnpm build
```

## Environment variables

| Name | Default | Purpose |
| :--- | :--- | :--- |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Base URL of the backend API |

Everything that starts with `VITE_` ends up in the JavaScript the browser downloads: never
put a secret in these variables.