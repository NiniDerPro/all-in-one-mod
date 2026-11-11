# Anonymous usage statistics

AIO keeps a private statistic locally (`config/aio/stats.json`, only on your own computer).
In addition it can send **anonymous technical numbers** to the project's own statistics server.
This is **off by default**: on the first start a notice on the title screen asks
"Share anonymous usage data for public statistics?" – **Okay** turns it on, **No thanks** keeps it off.
You can change this at any time under **Client → Send anonymous usage statistics**.

Data is only sent if the switch is on **and** the build contains a server address. The address is not in the
source code; official builds receive it at build time from a secret (`AIO_STATS_URL`), your own builds send nothing.

Full privacy information: [`PRIVACY.md`](../PRIVACY.md).

## What is sent

A package right after the start (about 90 s) and then every 10 minutes:

| Group | Fields |
|---|---|
| Installation | random installation id (not linked to account or device), schema version |
| Versions | mod, Minecraft, Fabric Loader and Java version, operating system (windows/macos/linux/other) |
| Usage | number of starts, total play time in groups (<1, 1-10, 10-50, 50-200, 200+ h), session length |
| Modules | list of active modules, count, peak, time in use per module in groups |
| Compatibility | which modules are off because of another mod (and which), server compatibility mode |
| Errors | modules with failing hooks, mixins that were not applied |
| Performance | average and minimum FPS of this session (sampled every 5 s) |

The server derives "live users" from an installation id having sent in the last ~15 minutes.

## What is never sent
Player name, UUID, account, IP address (the server does not store it), server names, coordinates, chat, files,
setting values, key bindings.

## Server
Cloudflare Worker + D1 database, source in `server/stats-worker/`. The address is kept out of the repository
(secret at build time). The worker accepts `POST` with JSON, stores the fields per installation id (last seen),
and deletes installations that have not been seen for 395 days. It must not store IP addresses or any headers
other than the ones it needs.
