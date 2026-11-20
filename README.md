# <img src="docs/favicon.svg" width="36" height="36" alt="Present logo" valign="middle"> Present

**Keep the people. Lose the feed.**

Present is a precision cognitive firewall for your phone and browser. You say what you want in plain words, for example *"Keep my Instagram DMs and YouTube subscriptions, but kill Reels and Shorts after 9 PM"*. Gemini compiles that sentence into a typed policy, you review it, and your phone enforces it on-device. Reels, Shorts, TikTok and Snapchat Spotlight go away; DMs, search and subscriptions stay.

Built for the **Google Startup Buildathon** by team **Hello Human**. Website: <https://ashish-codejourney.github.io/MindGuard/>

> Present grew out of MindGuard, an on-device Reels/Shorts blocker. The Android package id is still `com.mindguard`, and the repo may still carry the old name.

---

## Why

Most blockers are all-or-nothing: they lock the whole app. You need one DM, unblock "just for a minute", and the block is dead. Present removes only the algorithmic surface and leaves the useful parts of the app open.

| Finding | Source |
| --- | --- |
| Indians spend about 1 h/day on YouTube and about 1 h/day on Instagram | [Hansa Research 2026](https://indiantelevision.com/iworld/youtube-tops-indias-digital-attention-race-hansa-research-study-finds/), [Hansa via MediaNews4U](https://www.medianews4u.com/93-of-consumers-use-youtube-while-instagram-sees-nearly-one-hour-of-daily-engagement-hansa-research/) |
| 97% of Indians watch Reels daily | [Meta study](https://www.outlookbusiness.com/news/97-of-indians-watch-reels-daily-says-meta-study) |
| One in six Indians spends 1.2 h/day on short-form video | [Redseer](https://www.businessinsider.in/tech/news/one-in-six-indians-spends-1-2-hours-a-day-scrolling-through-shorts-reels-more-such/articleshow/105070572.cms) |
| A 10-second pause made people abandon 36% of app opens; opens fell 57% over 6 weeks | [one sec study, PNAS 2023](https://pmc.ncbi.nlm.nih.gov/articles/PMC9974409) |
| A "years of your life" projection is the most-shared screen in Opal's onboarding | [Opal 2023 Screen Time Report](https://opal.so/blog/opals-2023-screen-time-report) |
| Whole-app blockers have a short half-life: people unblock for one message | [DEV](https://dev.to/astraedus/block-youtube-shorts-and-reels-on-android-keep-the-app-1b9h), [Hacker News](https://hn.svelte.dev/item/48312443) |

---

## Features

### Intent to policy, with Gemini

Type an intent (or tap an example chip) and tap **Compile with Gemini**. The Cloud Run compiler returns either a policy or a plain-language reason it can't be enforced. The **review screen** shows a summary, green chips for what stays usable, red chips for what gets blocked, and the schedule. Nothing is enforced until you tap **Activate**. Gemini also gets your current policy, so you can make changes like "also block Snapchat Spotlight".

### Animated onboarding

About 90 seconds, one question per screen: which apps pull you in, how long per day (self-reported, or measured with optional Android Usage Access), how much of that is short-form, when it hits hardest, and what must stay usable. Then:

- **The mirror**: daily minutes, then hours per year, then **years of your waking life**, with a "how we calculate" breakdown and a share button.
- **The upside**: time back per week using a conservative 50% reduction, shown as real-world equivalents (gym sessions, evenings with friends).
- **Your plan**: an intent sentence written from your answers. You can edit it before Gemini compiles it.
- **Commit**: press and hold "I choose to be present", then the accessibility disclosure and a step-by-step permission guide.

### On-device enforcement (Android)

An Accessibility Service watches Instagram, YouTube, TikTok and Snapchat only, and leaves the short-video screen as soon as it loads.

| Platform | Surface blocked on Android | Stays usable |
| --- | --- | --- |
| Instagram | Reels (`reels_tab`) | Home, DMs, search, profiles |
| YouTube | Shorts player (`shorts_player`) | Search, subscriptions, regular videos |
| TikTok | Video feed (`reels_tab`) | n/a |
| Snapchat | Spotlight (`spotlight`) | Chat, camera |

What isn't enforced on Android yet:

- `home_feed` and `explore_grid` are web-only. The review screen tags them **web only**, and the Chrome extension enforces them.
- `daily_allowance` schedules are treated as "always" because there is no per-minute usage tracking yet.
- `whitelisted_entities` (allowed creators) are shown on the review screen but not enforced.
- The schema also accepts Reddit and LinkedIn, but neither the app nor the extension enforces them.

### Turn-off guard

Turning protection off (in the app or from the tile) is not instant. You wait out a countdown that starts at 15 s and doubles with each turn-off that day, up to 60 s. While you wait you get a real-world quest ("Call someone you miss", "Make a plan with a friend", "Step outside for 10 minutes", and others), some with a one-tap dial or message action. You then type a reason of at least 10 characters. You can also choose **Pause for 15 minutes instead**.

### Pause card

**I need 5 minutes** on Home opens a pause card: a countdown (the policy's `delay_seconds`, default 10 s) asking whether this is what you opened your phone for, a side quest (hydrate, rest your eyes, stretch) and a reason box. Then protection pauses for 5 minutes.

### Weekly Gemini insight

The Stats screen shows blocks, an approximate "time reclaimed" figure (4.2 min per intercept), and streaks. **Get my weekly insight** sends aggregate counts to Gemini, which returns a headline, an insight and a suggested rule.

### Quick Settings tile

Add the **Focus Mode** tile from the notification shade's tile editor. While paused, tapping it resumes protection. Otherwise it opens the turn-off guard.

### Chrome extension (MV3)

The desktop half lives in [`extension/`](extension/README.md). On youtube.com it hides Shorts shelves and links, sends `/shorts/<id>` to the normal watch page, and can hide the home feed. On instagram.com it hides the Reels and Explore links and redirects them to the DM inbox. The popup uses the same compiler and review flow, and the policy lives in `chrome.storage.sync`. With no policy saved, it hides Shorts and Reels by default.

---

## Architecture

```mermaid
flowchart LR
    subgraph Phone["Android app (Kotlin, Compose)"]
        UI[Intent + Review screens] --> Store[(PolicyStore<br/>DataStore JSON)]
        Store --> Svc[Accessibility Service]
        Svc --> Eval[PolicyEvaluator<br/>shared/ pure Kotlin]
    end
    subgraph Chrome["Chrome MV3 extension"]
        Popup[Popup] --> Sync[(chrome.storage.sync)]
        Sync --> CS[Content script<br/>CSS hide + redirects]
    end
    subgraph GCP["Google Cloud"]
        Run[Cloud Run: present-compiler<br/>Hono + @google/genai + Zod]
        SM[(Secret Manager<br/>GEMINI_API_KEY, PRESENT_API_KEY)]
        SM --> Run
    end
    Gemini[Gemini API<br/>structured output]

    UI -- "POST /v1/compile, /v1/insights<br/>x-present-key" --> Run
    Popup -- "POST /v1/compile" --> Run
    Run <--> Gemini
```

- **Compile online, enforce offline.** The network is only used to compile an intent and to fetch an insight. Each block decision is made on the device by `PolicyEvaluator.shouldEnforce(policy, package, surface, minuteOfDay)`, which supports `always` and `time_window` schedules, including overnight windows such as 21:00 to 06:00.
- **Compiler service** ([`server/`](server/README.md)): `POST /v1/compile`, `POST /v1/insights`, `GET /health`. It is live at `https://present-compiler-283760043967.asia-south1.run.app` (region `asia-south1`). Both keys come from Secret Manager. It calls Gemini with an AI Studio key, not Vertex AI, because Vertex rejected the newer Gemini models on a trial billing account. With a paid account it can switch to Vertex with `GOOGLE_GENAI_USE_VERTEXAI=true`. Models are tried in a fallback chain (`GEMINI_MODELS`, default `gemini-3.1-flash-lite,gemini-3.5-flash`) with a 6 s timeout each.

### Policy schema

The schema is defined once in Zod ([`server/src/policy.ts`](server/src/policy.ts)). It is exported to JSON Schema and passed to Gemini as `responseJsonSchema`. The extension imports the same file, and Kotlin mirrors it in [`shared/.../policy/PresentPolicy.kt`](shared/src/commonMain/kotlin/com/mindguard/shared/policy/PresentPolicy.kt).

```json
{
  "policy_version": "1.0",
  "summary": "Instagram DMs stay open. Reels are blocked from 9 PM to 6 AM.",
  "rules": [{
    "platform": "instagram",
    "target_surfaces": ["reels_tab"],
    "allowed_surfaces": ["direct_messages", "search"],
    "schedule": { "mode": "time_window", "start_time": "21:00", "end_time": "06:00" }
  }],
  "pause_card_settings": { "delay_seconds": 10, "side_quest_category": "hydration" }
}
```

| Field | Values |
| --- | --- |
| `platform` | `instagram`, `youtube`, `tiktok`, `snapchat`, `reddit`, `linkedin` |
| `target_surfaces` | `reels_tab`, `shorts_player`, `home_feed`, `explore_grid`, `spotlight` |
| `allowed_surfaces` | `direct_messages`, `search`, `subscriptions`, `profile`, `creator_channel` |
| `schedule.mode` | `always`, `time_window` (`start_time`/`end_time` HH:MM), `daily_allowance` (`daily_limit_minutes`) |

If an intent can't be enforced, the compiler returns `{ "status": "rejected", "reason": "..." }` instead of a policy.

---

## Privacy

| | |
| --- | --- |
| **Read on the device** | Only inside Instagram, YouTube, TikTok and Snapchat (the accessibility scope is limited to these packages): the view tree of the current screen (resource IDs, class names, labels) so the app can tell which screen is open. It is processed in memory and is not stored or sent anywhere. |
| **Never read** | Anything in other apps, screen pixels and your photos. |
| **Stored on the device** | Your active policy, onboarding answers, block counts and streaks (DataStore). |
| **Leaves the device** | (1) The intent text you compile, plus your current policy so Gemini can edit it. (2) If you tap **Get my weekly insight**: aggregate counts only (intercepts per app, total intercepts, streak days). Both go to the Present compiler on Cloud Run and from there to Gemini. |

There are no accounts and no analytics. Calls are authenticated with a shared `x-present-key` header (there is no per-user auth yet).

---

## Google tech used

| Technology | Use |
| --- | --- |
| **Gemini API** (`@google/genai`, structured output with a JSON Schema) | Intent → policy compiler; weekly insight |
| **Cloud Run** | Hosts the `present-compiler` service (built from source with Cloud Build into Artifact Registry) |
| **Secret Manager** | Holds `GEMINI_API_KEY` and `PRESENT_API_KEY` for Cloud Run |
| **Android** (Accessibility Service, Quick Settings tile, Usage Stats, Jetpack Compose, DataStore) | On-device enforcement and the app |
| **Chrome** (Manifest V3) | Desktop extension |
| **Kotlin Multiplatform** | Pure, JVM-testable policy engine and detection rules in `shared/` |

---

## Getting started

### Install the app

1. Download the APK. The latest build is the `mindguard-debug-apk` artifact on the most recent green [CI run](../../actions/workflows/ci.yml) on `main`. Tagged builds are attached to [Releases](../../releases). Debug builds are signed with a committed debug keystore, so a new build installs over the old one.
2. Install it and go through onboarding.
3. **Android 13+ only, for sideloaded apps**: go to Settings → Apps → Present → ⋮ (top right) → **Allow restricted settings**.
4. Go to Settings → Accessibility → **Present** and turn it on. The onboarding permission step walks you through this and notices when it's done.
5. Write your first intent, for example *"Keep Instagram DMs, kill Reels and Shorts after 9 PM"*, then tap **Compile with Gemini** and **Activate**. Open Reels to test it.

Requires Android 7.0+ (API 24).

### For developers

| Path | What's there |
| --- | --- |
| [`shared/`](shared/) | Kotlin Multiplatform: detection rules, `PresentPolicy`, `PolicyEvaluator`, `PolicyJson` |
| [`androidApp/`](androidApp/) | Compose UI, onboarding, accessibility service, tile, `CompilerClient` |
| [`server/`](server/) | Cloud Run compiler (TypeScript, Hono, `@google/genai`, Zod) |
| [`extension/`](extension/) | Chrome MV3 extension (TypeScript, esbuild) |
| [`docs/`](docs/) | Website (GitHub Pages) |
| [`plans/`](plans/) | Plans and design docs |

**Run the compiler locally**

```sh
cd server
npm install
npm test
printf 'GEMINI_API_KEY=...\nPRESENT_API_KEY=dev-key\n' > .env   # .env is git-ignored
npm run build
node --env-file=.env dist/server.js                          # listens on :8080
```

For hot reload, use `npm run dev` with the same variables exported.

**Point the Android app at it.** The app reads `present.apiUrl` and `present.apiKey` from `local.properties`. The defaults are the live Cloud Run URL and `dev-key`.

```properties
present.apiUrl=http://localhost:8080
present.apiKey=dev-key
```

```sh
adb reverse tcp:8080 tcp:8080   # phone's localhost:8080 -> your machine
```

Cleartext HTTP is allowed only for `localhost` and `10.0.2.2` (the emulator).

**Build the extension**

```sh
cd extension
npm install
npm test && npm run typecheck
npm run build   # load extension/dist/ via chrome://extensions → Load unpacked
```

Set the service URL and key in the popup's **Settings**.

**Android builds.** CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) is the source of truth. It uses JDK 17, runs `./gradlew build` and the shared tests, and uploads the debug APK. It writes `present.apiKey` from the `PRESENT_API_KEY` Actions secret, so CI builds can call the live service. Locally, with an Android SDK and JDK 17:

```sh
./gradlew :androidApp:assembleDebug   # androidApp/build/outputs/apk/debug/
```

---

## Testing

The project is built test-first (TDD). Logic lives in pure functions and is tested through public behaviour.

| Where | What | Run |
| --- | --- | --- |
| `shared/src/commonTest` | Detection rules, `PolicyEvaluator` (including overnight windows), policy JSON, Gherkin-style acceptance tests per app | `./gradlew :shared:testDebugUnitTest` |
| `androidApp/src/androidUnitTest` | Onboarding projection and intent sentence, turn-off guard, policy formatting, insights request, watchdog, Instagram home-tab strategy | `./gradlew :androidApp:testDebugUnitTest` |
| `server/src/*.test.ts` | Compile/insights endpoints, API key check, Gemini fallback chain (Vitest, fake Gemini) | `cd server && npm test` |
| `extension/src/core/*.test.ts` | CSS rules, redirects, schedules, compile client, review, settings, stats (Vitest) | `cd extension && npm test` |

---

## Roadmap

From [`plans/`](plans/):

- Enforce `daily_allowance` with real per-minute usage tracking, on Android and in the extension
- Enforce `whitelisted_entities` (allowed creators and channels)
- Per-user auth (Firebase Auth) instead of the shared `x-present-key`
- Move Gemini calls to Vertex AI with the service account (no stored Gemini key) once billing allows
- Feed the onboarding baseline into the weekly insight ("down 42% from your starting 1 h 48 m") and weight turn-off quests by what you said you'd do with the time
- Home progress against your projection ("You've reclaimed 3 h 20 m of your 6.3 years")

---

## Team

**Hello Human**: Jay Dobariya, Ashish Vaghela, Dhruvi Jha.

## Licence

MIT. See [LICENSE](LICENSE).
