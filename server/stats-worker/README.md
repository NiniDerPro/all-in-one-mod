# Stats worker (Cloudflare Workers + D1)

Receives the anonymous pings described in `docs/TELEMETRY.md`.

- `POST /v1/ping` – public, validates and stores one install's latest numbers (no IP is stored).
- `GET /v1/stats` – evaluation, needs `Authorization: Bearer <ADMIN_TOKEN>`.

Bindings: D1 database `DB`, secret `ADMIN_TOKEN`. Create the tables with `schema.sql`.
Deploy: `wrangler deploy` (config: name `aio-stats`, `main = "worker.js"`, D1 binding `DB`).
