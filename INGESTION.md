## Events and Logs Ingestion API

This document describes how to ingest application logs and analytics events into the backend using Supabase Edge Functions. Use this as a reference when building an independent SDK.

### Overview

- Base URL: `https://tgkcqvkyxpephsbhrkqm.functions.supabase.co`
- Auth: per-app secret `apiKey` (generated on app creation and stored in `apps.api_key`)
- Data isolation: requests are scoped to the app resolved by `apiKey`; the server assigns `app_id` automatically

> Note: Do not include fields that are not part of the documented schemas below. Unknown fields will cause the insert to fail.

---

### Logs

- Endpoint: `POST /sdk-logs`
- URL: `https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/sdk-logs`

Request body (JSON):

```json
{
  "apiKey": "<APP_API_KEY>",
  "logs": [
    {
      "level": "info",
      "message": "Application started",
      "created_at": "2025-07-13T12:34:56.000Z" // optional ISO 8601
    }
  ]
}
```

Allowed log item fields:
- `level` (string)
- `message` (string)
- `created_at` (ISO 8601 string, optional; defaults to server time if omitted)

Server behavior:
- Validates `apiKey` against `apps.api_key`
- Adds `app_id` for each row
- Inserts into `logs(id, app_id, level, message, created_at)`

Responses:
- `200 OK` with body `ok` on success
- `400 Bad Request` if payload is malformed
- `401 Unauthorized` if `apiKey` is invalid
- `405 Method Not Allowed` for non-POST methods
- `500 Internal Server Error` for database errors

Example (curl):

```bash
curl -X POST "https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/sdk-logs" \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "YOUR_APP_API_KEY",
    "logs": [
      { "level": "info", "message": "started" },
      { "level": "error", "message": "failed to fetch" }
    ]
  }'
```

---

### Events

- Endpoint: `POST /sdk-events`
- URL: `https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/sdk-events`

Request body (JSON):

```json
{
  "apiKey": "<APP_API_KEY>",
  "events": [
    {
      "name": "signup",
      "properties": { "plan": "pro" },
      "created_at": "2025-07-13T12:34:56.000Z" // optional ISO 8601
    }
  ]
}
```

Allowed event item fields:
- `name` (string)
- `properties` (object, stored as JSON; optional)
- `created_at` (ISO 8601 string, optional; defaults to server time if omitted)

Server behavior:
- Validates `apiKey` against `apps.api_key`
- Adds `app_id` for each row
- Inserts into `events(id, app_id, name, properties, created_at)`

Responses:
- `200 OK` with body `ok` on success
- `400 Bad Request` if payload is malformed
- `401 Unauthorized` if `apiKey` is invalid
- `405 Method Not Allowed` for non-POST methods
- `500 Internal Server Error` for database errors

Example (curl):

```bash
curl -X POST "https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/sdk-events" \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "YOUR_APP_API_KEY",
    "events": [
      { "name": "signup", "properties": { "plan": "pro" } },
      { "name": "screen_view", "properties": { "screen": "Home" } }
    ]
  }'
```

---

### Remote Flags

- Endpoint: `GET /sdk-flags`
- URL: `https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/sdk-flags`
- Purpose: retrieve the app’s remote configuration flags stored in the backend.

Request options:
- Send `apiKey` either as a query param or as a header.
  - Query: `GET /sdk-flags?apiKey=<APP_API_KEY>`
  - Header: `x-api-key: <APP_API_KEY>`

Response body (JSON):

```json
{
  "flags": [
    { "key": "feature.newOnboarding", "value": "true" },
    { "key": "api.baseUrl", "value": "https://api.example.com" }
  ]
}
```

Notes:
- Flags are sourced from the `remote_flags` table for the app identified by `apiKey` (server resolves `app_id`).
- `value` is stored as text in the database. Parse to boolean/number/JSON in your SDK as needed.

Responses:
- `200 OK` with `{ flags: Array<{ key: string; value: string }> }`
- `400 Bad Request` if `apiKey` is missing
- `401 Unauthorized` if `apiKey` is invalid
- `500 Internal Server Error` for database errors

Examples (curl):

```bash
# Query param
curl "https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/sdk-flags?apiKey=YOUR_APP_API_KEY"

# Header
curl -H "x-api-key: YOUR_APP_API_KEY" "https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/sdk-flags"
```

Minimal TypeScript helper:

```ts
export type RemoteFlag = { key: string; value: string };

export async function getRemoteFlags(apiKey: string): Promise<RemoteFlag[]> {
  const res = await fetch(`https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/sdk-flags`, {
    headers: { 'x-api-key': apiKey }
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${text}`);
  }
  const body = (await res.json()) as { flags: RemoteFlag[] };
  return body.flags ?? [];
}
```

Caching guidance:
- Flags usually change infrequently. Consider caching in-memory for a short TTL (e.g., 30–120 seconds) and refreshing in the background.
- Provide a simple key/value lookup API in your SDK and handle type parsing centrally (e.g., `getBoolean("feature.x")`).

Reference implementation: see `supabase/functions/sdk-flags/index.ts`.

---

### Minimal client example (TypeScript)

```ts
type LogItem = {
  level?: string;
  message?: string;
  created_at?: string; // ISO string
};

type EventItem = {
  name: string;
  properties?: Record<string, unknown>;
  created_at?: string; // ISO string
};

async function postJson(path: string, body: unknown) {
  const res = await fetch(`https://tgkcqvkyxpephsbhrkqm.functions.supabase.co/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${text}`);
  }
}

export async function sendLogs(apiKey: string, logs: LogItem[]) {
  // Only known fields: level, message, created_at
  return postJson('sdk-logs', { apiKey, logs });
}

export async function sendEvents(apiKey: string, events: EventItem[]) {
  // Only known fields: name, properties, created_at
  return postJson('sdk-events', { apiKey, events });
}
```

---

### Practical guidance for SDKs

- Keep payloads small and batch reasonable numbers of items per request.
- Populate `created_at` with client time if you need precise ordering; otherwise omit to use server time.
- Retry only on `5xx` with exponential backoff; do not retry `4xx`.
- Treat the `apiKey` as a secret; do not embed it in publicly distributed code without precautions.
- If you need to extend data captured for logs, add a `metadata jsonb` column to the `logs` table and include it in payloads. For events, put extras under `properties`.

---

### Reference (schema snippets)

```sql
-- logs
create table logs (
  id uuid primary key default gen_random_uuid(),
  app_id uuid references apps(id) on delete cascade,
  level text,
  message text,
  created_at timestamp default now()
);

-- events
create table events (
  id uuid primary key default gen_random_uuid(),
  app_id uuid references apps(id) on delete cascade,
  name text,
  properties jsonb,
  created_at timestamp default now()
);
```


