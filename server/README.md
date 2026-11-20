# present-compiler

Cloud Run service that turns a plain-language intent into a Present policy with Gemini structured output.

| Endpoint | Purpose |
| --- | --- |
| `POST /v1/compile` | `{ raw_prompt, existing_policy? }` → 200 `{status:"compiled", policy}` · 422 `{status:"rejected", reason}` · 400/401/502 `{status:"error", message}` |
| `POST /v1/insights` | `{ intercepts_by_app, streak_days, total_intercepts }` → 200 `{ headline, insight, suggestion }` |
| `GET /healthz` | Liveness |

All `/v1` calls need the header `x-present-key: $PRESENT_API_KEY`. The policy schema is in `src/policy.ts`.

## Run locally

```sh
npm install
npm test
GEMINI_API_KEY=... PRESENT_API_KEY=dev-key npm run dev
curl -s localhost:8080/v1/compile -H 'content-type: application/json' -H 'x-present-key: dev-key' \
  -d '{"raw_prompt":"Keep Instagram DMs, kill Reels and Shorts after 9 PM"}'
```

## Deploy to Cloud Run (Gemini through Vertex AI, so no API key is stored)

```sh
PROJECT=your-project-id
gcloud services enable run.googleapis.com aiplatform.googleapis.com cloudbuild.googleapis.com --project $PROJECT
gcloud run deploy present-compiler --source . --project $PROJECT --region asia-south1 --allow-unauthenticated \
  --set-env-vars GOOGLE_GENAI_USE_VERTEXAI=true,GOOGLE_CLOUD_PROJECT=$PROJECT,GOOGLE_CLOUD_LOCATION=us-central1,PRESENT_API_KEY=$(openssl rand -hex 16)
# Let the service account call Gemini (Cloud Run uses the default compute service account unless you set one):
SA=$(gcloud projects describe $PROJECT --format='value(projectNumber)')-compute@developer.gserviceaccount.com
gcloud projects add-iam-policy-binding $PROJECT --member serviceAccount:$SA --role roles/aiplatform.user
```

To skip Vertex, set `GEMINI_API_KEY` instead of the three `GOOGLE_*` variables. `GEMINI_MODELS` is a comma-separated fallback chain (default `gemini-3.1-flash-lite,gemini-3.5-flash`). Each model gets `GEMINI_TIMEOUT_MS` (default 6000). If it is overloaded or slow, the next one is tried.
