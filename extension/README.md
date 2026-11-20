# Present for Chrome

Desktop half of Present, by Hello Human. It hides algorithmic short-video surfaces (YouTube Shorts, Instagram Reels and Explore) and leaves search, subscriptions and DMs alone. The popup sends your intent to the Present compiler (`server/`) and stores the resulting policy in `chrome.storage.sync`.

## Build

```sh
cd extension
npm install
npm test          # pure core: CSS rules, redirects, schedules, compile client
npm run typecheck
npm run build     # bundles into extension/dist/
```

The policy schema is imported from `../server/src/policy.ts`, so there is one source of truth.

## Load

1. Open `chrome://extensions` and turn on **Developer mode**.
2. Click **Load unpacked** and pick `extension/dist/`.
3. Pin **Present**. With no policy saved, it hides Shorts and Reels by default.

## Use

1. Start the compiler (`cd server && npm run dev`) or deploy it to Cloud Run.
2. In the popup, open **Settings** and set the service URL (default `http://localhost:8080`) and key (default `dev-key`).
3. Describe what you want, click **Compile with Gemini**, review, then **Activate**.

The manifest allows requests to `localhost`, `127.0.0.1` and `*.run.app`. Add any other host to `host_permissions` in `static/manifest.json`.

Notes: `daily_allowance` rules block all day on desktop (no minute tracking yet). `whitelisted_entities` are not enforced yet.
