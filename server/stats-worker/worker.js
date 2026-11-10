const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const MOD_ID = /^[a-z0-9_]{1,40}$/;
const s = (v, n = 40) => (typeof v === "string" ? v.slice(0, n) : null);
const i = (v) => (Number.isFinite(v) ? Math.max(0, Math.min(1e9, Math.trunc(v))) : null);
const json = (o, status = 200) => new Response(JSON.stringify(o), { status, headers: { "content-type": "application/json", "access-control-allow-origin": "*" } });

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    if (req.method === "POST" && url.pathname === "/v1/ping") return ping(req, env);
    if (req.method === "GET" && url.pathname === "/v1/stats") return stats(req, env);
    return json({ error: "not found" }, 404);
  },
};

async function ping(req, env) {
  const text = await req.text();
  if (text.length > 20000) return json({ error: "too large" }, 413);
  let p;
  try { p = JSON.parse(text); } catch { return json({ error: "bad json" }, 400); }
  if (p.schema !== 1 || typeof p.installId !== "string" || !UUID.test(p.installId)) return json({ error: "bad payload" }, 400);
  const now = Date.now();
  const id = p.installId.toLowerCase();
  const v = p.versions || {}, u = p.usage || {}, m = p.modules || {}, c = p.compat || {}, f = p.performance || {};

  const prev = await env.DB.prepare("SELECT last_seen FROM installs WHERE install_id=?").bind(id).first();
  if (prev && now - prev.last_seen < 60000) return json({ ok: true, skipped: true });

  const clean = {
    versions: { mod: s(v.mod), minecraft: s(v.minecraft), loader: s(v.loader), java: s(v.java, 10), os: s(v.os, 10) },
    usage: { launches: i(u.launches), playHours: s(u.playHours, 10), sessionMinutes: i(u.sessionMinutes) },
    performance: { avgFps: i(f.avgFps), minFps: i(f.minFps), samples: i(f.samples) },
  };
  const stmts = [
    env.DB.prepare(`INSERT INTO installs (install_id, first_seen, last_seen, mod_version, mc_version, loader_version, java, os, launches, play_hours, session_minutes, active_count, peak_active, avg_fps, min_fps, compat_mode, payload)
      VALUES (?1,?2,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16)
      ON CONFLICT(install_id) DO UPDATE SET last_seen=?2, mod_version=?3, mc_version=?4, loader_version=?5, java=?6, os=?7, launches=?8, play_hours=?9, session_minutes=?10, active_count=?11, peak_active=?12, avg_fps=?13, min_fps=?14, compat_mode=?15, payload=?16`)
      .bind(id, now, clean.versions.mod, clean.versions.minecraft, clean.versions.loader, clean.versions.java, clean.versions.os,
        clean.usage.launches, clean.usage.playHours, clean.usage.sessionMinutes, i(m.activeCount), i(m.peakActive),
        clean.performance.avgFps, clean.performance.minFps, s(c.compatMode, 12), JSON.stringify(clean)),
    env.DB.prepare("DELETE FROM module_use WHERE install_id=?").bind(id),
    env.DB.prepare("DELETE FROM issues WHERE install_id=?").bind(id),
  ];
  const active = new Set((Array.isArray(m.active) ? m.active : []).filter((x) => typeof x === "string" && MOD_ID.test(x)).slice(0, 100));
  const hours = m.hours && typeof m.hours === "object" ? m.hours : {};
  const ids = new Set([...active, ...Object.keys(hours).filter((x) => MOD_ID.test(x))].slice(0, 120));
  for (const mod of ids) stmts.push(env.DB.prepare("INSERT INTO module_use (install_id, module_id, active, hours, last_seen) VALUES (?,?,?,?,?)").bind(id, mod, active.has(mod) ? 1 : 0, s(hours[mod], 10), now));
  const dt = c.deferredTo && typeof c.deferredTo === "object" ? c.deferredTo : {};
  for (const [mod, other] of Object.entries(dt).slice(0, 60)) if (MOD_ID.test(mod)) stmts.push(env.DB.prepare("INSERT OR REPLACE INTO issues (install_id, kind, name, detail, last_seen) VALUES (?,?,?,?,?)").bind(id, "deferred", mod, s(other, 60), now));
  for (const mod of (Array.isArray(c.brokenHooks) ? c.brokenHooks : []).slice(0, 60)) if (typeof mod === "string" && MOD_ID.test(mod)) stmts.push(env.DB.prepare("INSERT OR REPLACE INTO issues (install_id, kind, name, detail, last_seen) VALUES (?,?,?,?,?)").bind(id, "brokenHook", mod, null, now));
  for (const mx of (Array.isArray(c.missingMixins) ? c.missingMixins : []).slice(0, 60)) if (typeof mx === "string") stmts.push(env.DB.prepare("INSERT OR REPLACE INTO issues (install_id, kind, name, detail, last_seen) VALUES (?,?,?,?,?)").bind(id, "missingMixin", s(mx, 60), null, now));
  await env.DB.batch(stmts);
  return json({ ok: true });
}

async function stats(req, env) {
  const auth = req.headers.get("authorization") || "";
  if (!env.ADMIN_TOKEN || auth !== "Bearer " + env.ADMIN_TOKEN) return json({ error: "unauthorized" }, 401);
  const now = Date.now();
  const q = (sql, ...b) => env.DB.prepare(sql).bind(...b).all().then((r) => r.results);
  const one = (sql, ...b) => env.DB.prepare(sql).bind(...b).first();
  const [total, live, d1, d7, d30] = await Promise.all([
    one("SELECT COUNT(*) n FROM installs"),
    one("SELECT COUNT(*) n FROM installs WHERE last_seen > ?", now - 15 * 60000),
    one("SELECT COUNT(*) n FROM installs WHERE last_seen > ?", now - 86400000),
    one("SELECT COUNT(*) n FROM installs WHERE last_seen > ?", now - 7 * 86400000),
    one("SELECT COUNT(*) n FROM installs WHERE last_seen > ?", now - 30 * 86400000),
  ]);
  return json({
    installs: { total: total.n, live: live.n, active24h: d1.n, active7d: d7.n, active30d: d30.n },
    versions: await q("SELECT mod_version, mc_version, COUNT(*) n FROM installs GROUP BY mod_version, mc_version ORDER BY n DESC"),
    java: await q("SELECT java, COUNT(*) n FROM installs GROUP BY java ORDER BY n DESC"),
    os: await q("SELECT os, COUNT(*) n FROM installs GROUP BY os ORDER BY n DESC"),
    modulesActive: await q("SELECT module_id, SUM(active) active, COUNT(*) seen FROM module_use GROUP BY module_id ORDER BY active DESC"),
    modulesUsage: await q("SELECT module_id, hours, COUNT(*) n FROM module_use WHERE hours IS NOT NULL GROUP BY module_id, hours ORDER BY module_id"),
    compat: await q("SELECT kind, name, detail, COUNT(*) n FROM issues GROUP BY kind, name, detail ORDER BY n DESC LIMIT 100"),
    performance: await one("SELECT ROUND(AVG(avg_fps)) avgFps, MIN(min_fps) minFps FROM installs WHERE avg_fps > 0"),
    peakActive: await one("SELECT MAX(peak_active) max, ROUND(AVG(peak_active),1) avg FROM installs"),
  });
}
