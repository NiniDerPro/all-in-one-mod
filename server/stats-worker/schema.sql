CREATE TABLE IF NOT EXISTS installs (
  install_id TEXT PRIMARY KEY,
  first_seen INTEGER NOT NULL,
  last_seen INTEGER NOT NULL,
  mod_version TEXT, mc_version TEXT, loader_version TEXT, java TEXT, os TEXT,
  launches INTEGER, play_hours TEXT, session_minutes INTEGER,
  active_count INTEGER, peak_active INTEGER,
  avg_fps INTEGER, min_fps INTEGER, compat_mode TEXT,
  payload TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS module_use (
  install_id TEXT NOT NULL,
  module_id TEXT NOT NULL,
  active INTEGER NOT NULL,
  hours TEXT,
  last_seen INTEGER NOT NULL,
  PRIMARY KEY (install_id, module_id)
);
CREATE TABLE IF NOT EXISTS issues (
  install_id TEXT NOT NULL,
  kind TEXT NOT NULL,
  name TEXT NOT NULL,
  detail TEXT,
  last_seen INTEGER NOT NULL,
  PRIMARY KEY (install_id, kind, name)
);
CREATE INDEX IF NOT EXISTS idx_installs_last ON installs(last_seen);
