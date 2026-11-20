# present-compiler

Cloud Run service that turns a plain-language intent into a Present policy with Gemini structured output.

| Endpoint | Purpose |
| --- | --- |
| `POST /v1/compile` | `{ raw_prompt, existing_policy? }` → 200 `{status:"compiled", policy}` · 422 `{status:"rejected", reason}` · 400/401/502 `{status:"error", message}` |
| `POST /v1/insights` | `{ intercepts_by_app, streak_days, total_intercepts }` → 200 `{ headline, insight, suggestion }` |
| `GET /health` | Liveness (not `/healthz`: Cloud Run reserves paths ending in `z`) |

All `/v1` calls need the header `x-present-key: $PRESENT_API_KEY`. The policy schema is in `src/policy.ts`.

## Run locally

```sh
npm install
npm test
GEMINI_API_KEY=... PRESENT_API_KEY=dev-key npm run dev
curl -s localhost:8080/v1/compile -H 'content-type: application/json' -H 'x-present-key: dev-key' \
  -d '{"raw_prompt":"Keep Instagram DMs, kill Reels and Shorts after 9 PM"}'
```

## Deploy to Cloud Run

Live: `https://present-compiler-283760043967.asia-south1.run.app` (project `present-hellohuman-1120`, region `asia-south1`).

Both keys live in Secret Manager; the Cloud Run runtime service account has `roles/secretmanager.secretAccessor` on them.

```sh
PROJECT=present-hellohuman-1120
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com --project $PROJECT
printf '%s' "$GEMINI_API_KEY" | gcloud secrets create gemini-api-key --project $PROJECT --data-file=-
openssl rand -hex 16 | tr -d '\n' | gcloud secrets create present-api-key --project $PROJECT --data-file=-
SA=$(gcloud projects describe $PROJECT --format='value(projectNumber)')-compute@developer.gserviceaccount.com
for s in gemini-api-key present-api-key; do
  gcloud secrets add-iam-policy-binding $s --project $PROJECT --member serviceAccount:$SA --role roles/secretmanager.secretAccessor
done
gcloud run deploy present-compiler --source . --project $PROJECT --region asia-south1 --allow-unauthenticated \
  --set-secrets GEMINI_API_KEY=gemini-api-key:latest,PRESENT_API_KEY=present-api-key:latest \
  --memory 512Mi --max-instances 3
```

We call Gemini with an AI Studio key rather than Vertex AI because Vertex rejected the newer Gemini models on a
trial billing account ("Access requires an eligible paid billing account"). With a paid account, switch to Vertex by
setting `GOOGLE_GENAI_USE_VERTEXAI=true`, `GOOGLE_CLOUD_PROJECT`, `GOOGLE_CLOUD_LOCATION=global` and granting the
service account `roles/aiplatform.user`; then no Gemini key is needed.

`GEMINI_MODELS` is a comma-separated fallback chain (default `gemini-3.1-flash-lite,gemini-3.5-flash`). Each model gets `GEMINI_TIMEOUT_MS` (default 6000). If it is overloaded or slow, the next one is tried.
