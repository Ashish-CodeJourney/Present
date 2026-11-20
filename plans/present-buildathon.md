# Plan: Present — Google Startup Buildathon (8 hours)

**Branch**: feat/present-buildathon
**Team**: Hello Human · **Product**: Present
**Source PRD**: HelloHuman_PRD.md (product name changed from "Hello Human" to "Present")

## Goal

Turn MindGuard (on-device Reels/Shorts blocker) into **Present**: the user types what they want in plain language
("Allow Instagram DMs, kill Reels and Shorts after 9 PM"), **Gemini on Google Cloud** turns it into a typed JSON
policy, the user approves it on a review screen, and the existing on-device engine enforces it.

## What already exists (reuse, don't rebuild)

| PRD module | MindGuard today | Gap |
| --- | --- | --- |
| FR-AND-01 Android engine | `MindGuardAccessibilityService` + `shared/rules/*` (IG Reels, YT Shorts, TikTok, Snapchat Spotlight), class-name + resource-ID detection, watchdog, cooldown | Engine is driven by on/off toggles + one hour-based focus window. It needs to be driven by a **policy** (per-platform surfaces + HH:MM schedule) |
| FR-TEL-01 Telemetry | `SettingsDataStore` per-app block counts, streaks, `StatsScreen` | Add "time reclaimed" figure; Gemini weekly insight |
| FR-PAUSE-01 Pause card | `pauseProtection()` exists but nothing in the UI calls it | Pause card + side quest + 5-min override with a reason |
| FR-COMP-01 Compiler | — | **New**: Cloud Run service + Gemini structured output |
| FR-REV-01 Review screen | — | **New**: Compose screen |
| FR-EXT-01 Chrome ext | — | **New**: MV3 extension |

## Decisions (where this plan departs from the PRD)

1. **Compiler in TypeScript (Hono + `@google/genai` + Zod), not FastAPI.** We write the policy schema once in Zod and
   the Chrome extension uses the same file. Zod 4 `z.toJSONSchema()` produces the `responseJsonSchema` we send to Gemini.
2. **Model**: `GEMINI_MODEL` env var, default `gemini-2.5-flash`. `gemini-1.5-flash` in the PRD has been retired.
   On Cloud Run we call through **Vertex AI** (`GOOGLE_GENAI_USE_VERTEXAI=true`) with the service account, so no API
   key is stored. Locally we use `GEMINI_API_KEY`.
3. **No Firebase Auth in the MVP.** A shared `x-present-key` header is enough for a demo. Auth is in the roadmap slide.
4. **Package id stays `com.mindguard`.** Only the user-facing name changes to "Present". Renaming the package mid-hackathon
   risks breaking the accessibility service registration.
5. **Enforceable surfaces in the MVP**: `reels_tab`, `shorts_player`, `spotlight` (we already detect these).
   `home_feed` / `explore_grid` are accepted by the schema but flagged "web only" on the review screen (the Chrome extension handles them).

## Architecture

```
Android (Present)  ──POST /v1/compile {raw_prompt}──▶  Cloud Run: present-compiler (TS)
  Review screen ◀── PresentPolicy JSON ─────────────  └─ Gemini (Vertex AI), structured output
  PolicyStore (DataStore JSON)                            + /v1/insights (weekly audit)
  AccessibilityService ─ PolicyEvaluator(policy, now, pkg) → block? (on-device, no network)
Chrome MV3 ext ── same /v1/compile ── content script hides Shorts/Reels selectors per policy
```

## 8-hour timeline (3 people in parallel)

| Hour | Ashish (backend + integration) | Jay (Android) | Dhruvi (extension, pitch, design) |
| --- | --- | --- | --- |
| 0–1 | Step 1: compiler service + schema (TDD) | Step 4: rebrand to Present, add kotlinx.serialization | Step 7: MV3 skeleton, YT/IG selectors |
| 1–2.5 | Step 2: deploy to Cloud Run, smoke test with curl | Step 5: `PresentPolicy` model + `PolicyEvaluator` in `shared` (TDD) | Step 7: popup with prompt box calling `/v1/compile` |
| 2.5–4.5 | Step 3: `/v1/insights` weekly audit | Step 6: Intent + Review screens, wire evaluator into service | Demo script, pitch deck, screenshots |
| 4.5–6 | Help Jay with the Android HTTP client + error states | Step 8: Pause card + side quest + 5-min override | Step 9: insights card on Stats screen |
| 6–7 | **Feature freeze.** End-to-end test on a real phone (IG + YT) | Bug fixes | Record backup demo video |
| 7–8 | Rehearse the pitch twice. No new code. | | |

## Steps (each one leaves the build working)

### Step 1 — Compiler service: schema + `/v1/compile` ✅ (this PR)
- `server/src/policy.ts`: Zod `PresentPolicy` schema (matches PRD §5.1) + JSON Schema export
- `server/src/compiler.ts`: `compilePolicy({ rawPrompt, existingPolicy, generate })`. `generate` is the Gemini port, so tests use a fake
- Reject unenforceable intents: Gemini returns `{ status: "rejected", reason }`
- `server/src/app.ts`: Hono app, `POST /v1/compile`, `GET /healthz`, API-key check
- **Done when**: `npm test` passes, `npm run dev` + curl returns a valid policy from real Gemini

### Step 2 — Deploy to Cloud Run
- `server/Dockerfile`, `gcloud run deploy present-compiler --source server --region asia-south1 --set-env-vars GOOGLE_GENAI_USE_VERTEXAI=true,GOOGLE_CLOUD_PROJECT=...,GOOGLE_CLOUD_LOCATION=us-central1,PRESENT_API_KEY=...`
- Grant the service account `roles/aiplatform.user`
- **Done when**: a curl to the Cloud Run URL answers in < 2 s

### Step 3 — `/v1/insights` (Gemini weekly cognitive audit)
- Input: `{ intercepts_by_app, streak_days, total_intercepts }` (counts only, no personal data). Output: `{ headline, insight, suggestion }`
- **Done when**: tested with a fake, then checked against real Gemini

### Step 4 — Rebrand to Present (Android)
- `strings.xml` app_name → "Present", accessibility description, onboarding copy, theme colours

### Step 5 — Policy model + evaluator in `shared` (TDD, pure Kotlin)
- `PresentPolicy` data classes (`@Serializable`) that mirror the Zod schema
- `PolicyEvaluator.shouldEnforce(policy, platform, surface, minuteOfDay): Boolean`. Handles `always`, and `time_window` including overnight windows (21:00→06:00)
- Map package → platform, rule → surface (IG Reels → `reels_tab`, YT Shorts → `shorts_player`, Snap → `spotlight`)
- Tests: overnight window, platform not in policy, surface not targeted, `always`

### Step 6 — Intent & Review screens, enforcement wiring
- `PolicyStore` (DataStore, JSON string) + `CompilerClient` (`HttpURLConnection`, so no new dependency)
- Intent screen: text box, three example chips, "Compile with Gemini" button, loading and error states
- Review screen: summary card, green chips for allowed surfaces, red chips for blocked ones, schedule line, **Activate** button
- `MindGuardAccessibilityService`: when an active policy exists, `PolicyEvaluator` decides, not the old toggles
- **Done when**: typing "No Reels after 9pm" on the phone at 21:05 bounces you out of Reels, and DMs still open

### Step 7 — Chrome extension (MV3)
- `extension/`: manifest, content script that injects CSS and runs a `MutationObserver` on youtube.com/instagram.com, popup that calls `/v1/compile`, policy kept in `chrome.storage.sync`

### Step 8 — Pause card + side quests
- When the user taps "I need 5 minutes": 10 s countdown ("Is this what you opened your phone for?"), pick 1 of 3 side quests, type a reason, then `pauseProtection(5 min)`

### Step 9 — Insights card on the Stats screen
- "Time reclaimed = intercepts × 4.2 min", plus a "Get my weekly insight" button that calls `/v1/insights`

## Cut list (drop these first if we're behind)
Step 9 → Step 8's side quests (keep the plain override) → Step 3 → extension popup (keep the hard-coded CSS hiding).
**Never cut**: Steps 1, 2, 5, 6. They are the demo.

## Demo script (90 s)
1. Instagram open on the phone, Reels autoplaying. "This is the trap."
2. Open Present, type *"Keep my Instagram DMs and YouTube subscriptions, but kill Reels and Shorts after 9 PM"* → Gemini → review screen in about 1 s
3. Activate → open Instagram → tap Reels → bounced out instantly → open DMs → works
4. Laptop: YouTube with no Shorts shelf
5. Stats: "47 Reels dodged, 3.3 h reclaimed" + Gemini insight

## Risks
- **Device**: accessibility on Android 13+ sideloads needs the "restricted settings" step. `PermissionsScreen` already handles it; rehearse it
- **Vertex quota or region**: keep a `GEMINI_API_KEY` fallback env var ready
- **Local env**: this machine has no Android SDK and only JDK 25 (Gradle 8.5 needs JDK 17–21). Android builds and tests run on Jay's machine or in CI
