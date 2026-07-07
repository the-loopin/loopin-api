# k6 Performance Tests

This folder contains k6 performance tests for the Loopin API. The suite is designed for local, CI, and disposable staging targets, not production data.

## Structure

```text
api-tests/k6
  smoke.js
  load.js
  stress.js
  spike.js
  helpers/
    auth.js
    config.js
    flows.js
    http.js
```

## Covered API Flows

- Public event discovery: `GET /events`, filtered event lists, and event detail when an event id is available.
- Public group detail: `GET /groups/{groupId}` when `PUBLIC_GROUP_ID` is provided.
- Authenticated user/profile reads: `GET /users/me`, `GET /me`, `GET /me/interests`, `GET /me/badges`.
- Authenticated recommendations: `GET /events/recommended`.
- Authenticated chat history: `GET /groups/{groupId}/messages` when `AUTH_GROUP_ID` is provided.
- Guarded write smoke: `POST /events`, only when `RUN_MUTATING_FLOWS=true` and `TARGET_ENV` is not production.

The default runs are read-only. Authenticated checks are skipped unless `AUTH_TOKEN` or `GOOGLE_ID_TOKEN` is provided.

## Install k6

Install k6 from <https://k6.io/docs/get-started/installation/>.

Verify:

```powershell
k6 version
```

## Configure Locally

Start the API locally, then load the example environment:

```powershell
Copy-Item api-tests/k6/.env.example api-tests/k6/.env
Get-Content api-tests/k6/.env | ForEach-Object {
  if ($_ -and -not $_.StartsWith("#")) {
    $name, $value = $_.Split("=", 2)
    Set-Item -Path "Env:$name" -Value $value
  }
}
```

The default target is `http://localhost:8080/api`.

## Run Tests

Smoke:

```powershell
k6 run api-tests/k6/smoke.js
```

Expected load:

```powershell
k6 run api-tests/k6/load.js
```

Stress:

```powershell
k6 run api-tests/k6/stress.js
```

Spike:

```powershell
k6 run api-tests/k6/spike.js
```

Authenticated run:

```powershell
$env:AUTH_TOKEN = "<local-or-ci-jwt>"
$env:REQUIRE_AUTH = "true"
k6 run api-tests/k6/smoke.js
```

If you only have a Google ID token, set `GOOGLE_ID_TOKEN`. The setup phase calls `POST /auth/google` and uses the returned JWT. Prefer `AUTH_TOKEN` in CI to avoid coupling performance tests to an external auth provider.

## Scenario Tuning

- Smoke validates basic stability with a tiny constant load.
- Load ramps to expected steady traffic and holds.
- Stress ramps beyond expected traffic to find saturation points.
- Spike jumps from `SPIKE_BASELINE_VUS` to `SPIKE_PEAK_VUS`, holds briefly, then drops back to baseline to measure recovery after sudden bursts.

## Results

k6 prints standard metrics for:

- Response time: `http_req_duration`
- Error rate: `http_req_failed`
- Throughput: `http_reqs`
- Checks: `checks`

The scripts also print a compact Loopin summary with p95, p99, error rate, throughput, and total requests.

Write a JSON summary artifact:

```powershell
$env:SUMMARY_JSON = "k6-summary.json"
k6 run api-tests/k6/load.js
```

Or use k6's native summary export:

```powershell
k6 run --summary-export k6-summary.json api-tests/k6/load.js
```

## Safety

- Do not point these tests at production services or production databases.
- The scripts fail fast when `TARGET_ENV=production` unless `ALLOW_PRODUCTION_TARGET=true` is set.
- `RUN_MUTATING_FLOWS=true` is blocked for production and should only be used with disposable local or CI data.
- Never commit real JWTs, Google ID tokens, API keys, or production URLs.