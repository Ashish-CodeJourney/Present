import { describe, expect, it } from "vitest";
import { createApp } from "./app.js";
import type { GenerateJson, GenerateRequest } from "./gemini.js";
import type { PresentPolicy } from "./policy.js";

const API_KEY = "test-key";

const aPolicy = (overrides: Partial<PresentPolicy> = {}): PresentPolicy => ({
  policy_version: "1",
  summary: "Reels and Shorts are blocked after 9 PM. DMs and subscriptions stay open.",
  rules: [
    {
      platform: "instagram",
      target_surfaces: ["reels_tab"],
      allowed_surfaces: ["direct_messages"],
      schedule: { mode: "time_window", start_time: "21:00", end_time: "06:00" },
      whitelisted_entities: [],
    },
  ],
  ...overrides,
});

const fakeGemini = (reply: unknown) => {
  const calls: GenerateRequest[] = [];
  const generate: GenerateJson = async (request) => {
    calls.push(request);
    return JSON.stringify(reply);
  };
  return { generate, calls };
};

const compile = (options: {
  generate: GenerateJson;
  body: unknown;
  apiKey?: string;
}) =>
  createApp({ generate: options.generate, apiKey: API_KEY }).request("/v1/compile", {
    method: "POST",
    headers: { "content-type": "application/json", "x-present-key": options.apiKey ?? API_KEY },
    body: JSON.stringify(options.body),
  });

describe("POST /v1/compile", () => {
  it("returns the policy Gemini compiled from the user's intent", async () => {
    const policy = aPolicy();
    const { generate } = fakeGemini({ status: "compiled", policy });

    const response = await compile({ generate, body: { raw_prompt: "No Reels after 9pm, keep DMs" } });

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({ status: "compiled", policy });
  });

  it("sends the user's words and the policy JSON schema to Gemini", async () => {
    const { generate, calls } = fakeGemini({ status: "compiled", policy: aPolicy() });

    await compile({ generate, body: { raw_prompt: "Kill all Shorts" } });

    expect(calls[0]?.prompt).toContain("Kill all Shorts");
    expect(JSON.stringify(calls[0]?.responseJsonSchema)).toContain("shorts_player");
  });

  it("includes the existing policy so Gemini can amend it instead of starting over", async () => {
    const existing = aPolicy({ summary: "EXISTING-POLICY-MARKER" });
    const { generate, calls } = fakeGemini({ status: "compiled", policy: aPolicy() });

    await compile({ generate, body: { raw_prompt: "Also block TikTok", existing_policy: existing } });

    expect(calls[0]?.prompt).toContain("EXISTING-POLICY-MARKER");
  });

  it("explains why an intent cannot be enforced on device", async () => {
    const reason = "We can't judge video quality without reading your screen. Whitelist creators instead.";
    const { generate } = fakeGemini({ status: "rejected", reason });

    const response = await compile({ generate, body: { raw_prompt: "Only show me high-quality videos" } });

    expect(response.status).toBe(422);
    expect(await response.json()).toEqual({ status: "rejected", reason });
  });

  it("refuses to pass on a policy that breaks the schema", async () => {
    const broken = { ...aPolicy(), rules: [{ platform: "myspace", target_surfaces: [], allowed_surfaces: [] }] };
    const { generate } = fakeGemini({ status: "compiled", policy: broken });

    const response = await compile({ generate, body: { raw_prompt: "Block MySpace" } });

    expect(response.status).toBe(502);
    expect(await response.json()).toMatchObject({ status: "error" });
  });

  it("refuses a time window that is not HH:MM", async () => {
    const policy = aPolicy({
      rules: [
        {
          platform: "youtube",
          target_surfaces: ["shorts_player"],
          allowed_surfaces: [],
          schedule: { mode: "time_window", start_time: "9pm", end_time: "25:00" },
        },
      ],
    });
    const { generate } = fakeGemini({ status: "compiled", policy });

    const response = await compile({ generate, body: { raw_prompt: "No Shorts after 9pm" } });

    expect(response.status).toBe(502);
  });

  it("returns 502 when Gemini replies with something that is not JSON", async () => {
    const generate: GenerateJson = async () => "sorry, I cannot help";

    const response = await compile({ generate, body: { raw_prompt: "Block Reels" } });

    expect(response.status).toBe(502);
  });

  it("returns 502 when Gemini is unreachable", async () => {
    const generate: GenerateJson = async () => {
      throw new Error("quota exceeded");
    };

    const response = await compile({ generate, body: { raw_prompt: "Block Reels" } });

    expect(response.status).toBe(502);
  });

  it("rejects an empty intent without calling Gemini", async () => {
    const { generate, calls } = fakeGemini({ status: "compiled", policy: aPolicy() });

    const response = await compile({ generate, body: { raw_prompt: "   " } });

    expect(response.status).toBe(400);
    expect(calls).toHaveLength(0);
  });

  it("rejects callers without the API key", async () => {
    const { generate, calls } = fakeGemini({ status: "compiled", policy: aPolicy() });

    const response = await compile({ generate, body: { raw_prompt: "Block Reels" }, apiKey: "wrong" });

    expect(response.status).toBe(401);
    expect(calls).toHaveLength(0);
  });
});

describe("GET /healthz", () => {
  it("reports the service is up without needing the API key", async () => {
    const { generate } = fakeGemini({});

    const response = await createApp({ generate, apiKey: API_KEY }).request("/healthz");

    expect(response.status).toBe(200);
  });
});

const askForInsight = (options: { generate: GenerateJson; body: unknown }) =>
  createApp({ generate: options.generate, apiKey: API_KEY }).request("/v1/insights", {
    method: "POST",
    headers: { "content-type": "application/json", "x-present-key": API_KEY },
    body: JSON.stringify(options.body),
  });

const weeklyStats = () => ({
  intercepts_by_app: { instagram: 52, youtube: 30, tiktok: 0, snapchat: 2 },
  streak_days: 4,
  total_intercepts: 84,
});

describe("POST /v1/insights", () => {
  it("returns Gemini's weekly cognitive audit", async () => {
    const insight = {
      headline: "84 Reels dodged",
      insight: "You won back about 5.9 hours this week, mostly from Instagram.",
      suggestion: "Try a 9:15 PM wind-down rule.",
    };
    const { generate } = fakeGemini(insight);

    const response = await askForInsight({ generate, body: weeklyStats() });

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual(insight);
  });

  it("gives Gemini the counts and the estimated time reclaimed at 4.2 minutes per intercept", async () => {
    const { generate, calls } = fakeGemini({ headline: "h", insight: "i", suggestion: "s" });

    await askForInsight({ generate, body: weeklyStats() });

    expect(calls[0]?.prompt).toContain('"instagram":52');
    expect(calls[0]?.prompt).toContain("353 minutes");
    expect(JSON.stringify(calls[0]?.responseJsonSchema)).toContain("suggestion");
  });

  it("rejects stats that are not counts", async () => {
    const { generate, calls } = fakeGemini({});

    const response = await askForInsight({ generate, body: { ...weeklyStats(), total_intercepts: -1 } });

    expect(response.status).toBe(400);
    expect(calls).toHaveLength(0);
  });

  it("returns 502 when Gemini's insight is malformed", async () => {
    const { generate } = fakeGemini({ headline: "only a headline" });

    const response = await askForInsight({ generate, body: weeklyStats() });

    expect(response.status).toBe(502);
  });
});

describe("browser callers", () => {
  it("answers the CORS preflight without the API key so the extension popup can call the API", async () => {
    const { generate } = fakeGemini({});

    const response = await createApp({ generate, apiKey: API_KEY }).request("/v1/compile", {
      method: "OPTIONS",
      headers: {
        origin: "chrome-extension://abc",
        "access-control-request-method": "POST",
        "access-control-request-headers": "content-type,x-present-key",
      },
    });

    expect(response.status).toBeLessThan(300);
    expect(response.headers.get("access-control-allow-headers")?.toLowerCase()).toContain("x-present-key");
  });
});
