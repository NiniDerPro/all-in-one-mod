const DASH = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta name=\"robots\" content=\"noindex\"><title>AIO stats</title>\n<style>\n:root{--bg:#0e1015;--card:#171a22;--line:#262b38;--t:#e8eaf0;--d:#8b93a7;--a:#7c5cff;--ok:#4ade80}\n*{box-sizing:border-box}body{margin:0 auto;background:var(--bg);color:var(--t);font:14px system-ui,sans-serif;padding:16px;max-width:1100px}\nh1{font-size:20px;margin:0 0 14px}h2{font-size:12px;color:var(--d);margin:0 0 10px;text-transform:uppercase;letter-spacing:.06em}\n.grid{display:grid;gap:12px;grid-template-columns:repeat(auto-fit,minmax(150px,1fr))}\n.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:14px}\n.big{font-size:30px;font-weight:700}.sub{color:var(--d);font-size:12px}\n.cols{display:grid;gap:12px;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));margin-top:12px}\n.row{display:flex;justify-content:space-between;gap:8px;margin:3px 0;position:relative;border-radius:4px;overflow:hidden}\n.bar{position:absolute;left:0;top:0;bottom:0;background:var(--a);opacity:.25}\n.row span{position:relative;padding:3px 6px}.row .n{color:var(--d)}\ninput,button{background:var(--card);color:var(--t);border:1px solid var(--line);border-radius:6px;padding:8px 10px;font:inherit}\nbutton{cursor:pointer;border-color:var(--a)}#err{color:#ff6b6b;margin-left:8px}.live{color:var(--ok)}\n</style></head><body>\n<h1>AIO ClientSide stats</h1>\n<div id=\"login\"><input id=\"tok\" type=\"password\" placeholder=\"Admin token\" autocomplete=\"off\"><button id=\"go\">Open</button><span id=\"err\"></span></div>\n<div id=\"app\" hidden>\n<div class=\"grid\" id=\"cards\"></div>\n<div class=\"cols\" id=\"cols\"></div>\n<p class=\"sub\" id=\"foot\"></p>\n</div>\n<script>\nvar tok=sessionStorage.getItem(\"aio_tok\")||\"\",timer=null;\nfunction el(t,c,x){var e=document.createElement(t);if(c)e.className=c;if(x!==undefined)e.textContent=x;return e}\nfunction card(box,title,big,sub){var c=el(\"div\",\"card\");c.appendChild(el(\"h2\",\"\",title));c.appendChild(el(\"div\",\"big\",String(big)));if(sub)c.appendChild(el(\"div\",\"sub\",sub));box.appendChild(c)}\nfunction list(box,title,rows,hint){\n  var c=el(\"div\",\"card\");c.appendChild(el(\"h2\",\"\",title));\n  if(!rows.length){c.appendChild(el(\"div\",\"sub\",\"no data yet\"))}\n  var max=1;rows.forEach(function(r){if(r.v>max)max=r.v});\n  rows.forEach(function(r){var row=el(\"div\",\"row\");var b=el(\"div\",\"bar\");b.style.width=Math.round(r.v/max*100)+\"%\";row.appendChild(b);row.appendChild(el(\"span\",\"\",r.k));row.appendChild(el(\"span\",\"n\",r.t||String(r.v)));c.appendChild(row)});\n  if(hint)c.appendChild(el(\"div\",\"sub\",hint));box.appendChild(c)}\nfunction render(d){\n  var cards=document.getElementById(\"cards\"),cols=document.getElementById(\"cols\");cards.textContent=\"\";cols.textContent=\"\";\n  var i=d.installs;\n  card(cards,\"Live now\",i.live,\"pinged in the last 15 min\");card(cards,\"Active 24 h\",i.active24h);card(cards,\"Active 7 days\",i.active7d);card(cards,\"Active 30 days\",i.active30d);card(cards,\"All installs\",i.total);\n  card(cards,\"Avg FPS\",d.performance&&d.performance.avgFps||0,\"lowest session min: \"+(d.performance&&d.performance.minFps||0));\n  card(cards,\"Modules on (avg peak)\",d.peakActive&&d.peakActive.avg||0,\"max \"+(d.peakActive&&d.peakActive.max||0));\n  var tot=Math.max(1,i.total);\n  list(cols,\"Modules switched on\",d.modulesActive.map(function(m){return{k:m.module_id,v:m.active,t:m.active+\" (\"+Math.round(m.active/tot*100)+\"%)\"}}));\n  var use={};d.modulesUsage.forEach(function(m){(use[m.module_id]=use[m.module_id]||[]).push(m.hours+\": \"+m.n)});\n  list(cols,\"Time in use per module (hours: installs)\",Object.keys(use).map(function(k){return{k:k,v:use[k].length,t:use[k].join(\"  \")}}));\n  list(cols,\"Versions (mod / Minecraft)\",d.versions.map(function(v){return{k:v.mod_version+\" / \"+v.mc_version,v:v.n}}));\n  list(cols,\"Java\",d.java.map(function(v){return{k:String(v.java),v:v.n}}));\n  list(cols,\"Operating system\",d.os.map(function(v){return{k:String(v.os),v:v.n}}));\n  var kinds={deferred:\"Module off, other mod wins\",brokenHook:\"Module disabled (hook failed)\",missingMixin:\"Mixin missing\"};\n  [\"deferred\",\"brokenHook\",\"missingMixin\"].forEach(function(k){\n    list(cols,kinds[k],d.compat.filter(function(x){return x.kind===k}).map(function(x){return{k:x.name+(x.detail?\" -> \"+x.detail:\"\"),v:x.n}}))});\n  document.getElementById(\"foot\").textContent=\"Updated \"+new Date().toLocaleTimeString()+\" - refreshes every 30 s\";\n}\nfunction load(){\n  fetch(\"/v1/stats\",{headers:{authorization:\"Bearer \"+tok}}).then(function(r){\n    if(!r.ok)throw 0;return r.json()}).then(function(d){\n    document.getElementById(\"login\").hidden=true;document.getElementById(\"app\").hidden=false;render(d);\n    sessionStorage.setItem(\"aio_tok\",tok);if(!timer)timer=setInterval(load,30000)\n  }).catch(function(){\n    sessionStorage.removeItem(\"aio_tok\");document.getElementById(\"app\").hidden=true;document.getElementById(\"login\").hidden=false;\n    document.getElementById(\"err\").textContent=\"Wrong token or no connection\"})\n}\ndocument.getElementById(\"go\").onclick=function(){tok=document.getElementById(\"tok\").value.trim();load()};\ndocument.getElementById(\"tok\").onkeydown=function(e){if(e.key===\"Enter\")document.getElementById(\"go\").onclick()};\nif(tok)load();\n</script></body></html>\n";
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
    if (req.method === "GET" && url.pathname === "/admin") return new Response(DASH, { headers: { "content-type": "text/html; charset=utf-8", "x-robots-tag": "noindex", "cache-control": "no-store" } });
    return new Response(null, { status: 404 });
  },
};

async function ping(req, env) {
  const text = await req.text();
  if (text.length > 20000) return json({ error: "too large" }, 413);
  let p;
  try { p = JSON.parse(text); } catch { return new Response(null, { status: 404 }); }
  if (p.schema !== 1 || typeof p.installId !== "string" || !UUID.test(p.installId)) return new Response(null, { status: 404 });
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
  if (!env.ADMIN_TOKEN || auth !== "Bearer " + env.ADMIN_TOKEN) return new Response(null, { status: 404 });
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
