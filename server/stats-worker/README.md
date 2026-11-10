# Stats worker (Cloudflare Workers + D1)

Receives the anonymous pings described in `docs/TELEMETRY.md`.

- `POST /v1/ping` – public, validates and stores one install's latest numbers (no IP is stored).
- `GET /v1/stats` – evaluation, needs `Authorization: Bearer <ADMIN_TOKEN>`.

Bindings: D1 database `DB`, secret `ADMIN_TOKEN`. Create the tables with `schema.sql`.
Deploy: `wrangler deploy` (config: name `aio-stats`, `main = "worker.js"`, D1 binding `DB`).

The worker answers everything except a valid ping / authorised stats request with an empty 404.
Its public address is kept out of the repository (build-time secret `AIO_STATS_URL`).

`GET /admin` serves a small dashboard page (`dashboard.html`, embedded in `worker.js`). The page
itself holds no data; it asks for the admin token and loads `/v1/stats`.
