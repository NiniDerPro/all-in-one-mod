# Privacy

AIO ClientSide works fully offline. Sending usage data is **optional and off by default**.

## Who is responsible
The developer of AIO ClientSide (GitHub: [niniderpro](https://github.com/NiniDerPro)).
Contact for all privacy questions and requests: open an issue at
<https://github.com/NiniDerPro/all-in-one-mod/issues>.

## Your choice
On the first start the game asks: "Share anonymous usage data for public statistics?"

- **Okay** turns sending on.
- **No thanks** keeps it off. Nothing is sent.

You can change this at any time under *AIO menu → Client settings → Send anonymous usage statistics*.
Switching it off stops all sending immediately.

## What is sent (only after "Okay")
Every 10 minutes while the game runs, a small package of technical numbers:

| Group | Data |
|---|---|
| Installation | a random installation ID (not linked to your account, name or device) |
| Versions | mod, Minecraft, Fabric Loader and Java version, operating system (windows / macos / linux / other) |
| Usage | number of game starts, total play time in rough groups (<1, 1-10, 10-50, 50-200, 200+ hours), length of the current session |
| Modules | which modules are switched on, how many, the highest number at once, rough time in use per module |
| Compatibility | which modules are turned off because another mod does the same job (and that mod's name), the server compatibility mode |
| Errors | modules whose game hooks failed, mixins that could not be applied |
| Performance | average and lowest FPS of the session |

## What is never sent
Your player name, UUID, account, IP address (it is not stored), server names or addresses,
coordinates, chat, files, setting values or key bindings.

## Where it is stored and for how long
On a server of the developer (Cloudflare Workers with a D1 database). Per installation only the latest
values are kept. Entries of installations that have not sent anything for about 13 months (395 days)
are deleted automatically. The data is used only for public, aggregated statistics (for example
"how many people use which module") and to find compatibility problems.

Technically the request passes Cloudflare's network; Cloudflare may process connection data as part
of running the service. The developer does not store IP addresses and has no access to them in the database.

## Access and deletion
The installation ID is in the file `config/aio/stats.json` on your computer (field `installId`).
To see or delete the data stored for your installation, open an issue (see above) and give the
installation ID, or ask for deletion without it by describing your request; since the data is
anonymous, it can only be found with the ID. You can also simply turn sending off and delete
`stats.json`: a new random ID is created if you ever turn it on again.

The installation ID is a random value, not personal information. Please still prefer to share it only
for deletion or access requests.

## Legal basis
Consent (GDPR Art. 6(1)(a)), given by clicking "Okay". You can withdraw it at any time by switching the
setting off.

## Changes
If the data changes, this file and the in-game notice are updated, and the notice is shown again.
