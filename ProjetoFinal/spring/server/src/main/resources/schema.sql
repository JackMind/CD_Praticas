CREATE TABLE IF NOT EXISTS server_data (
    key TEXT PRIMARY KEY,
    invalidate BOOLEAN,
    data jsonb
);
