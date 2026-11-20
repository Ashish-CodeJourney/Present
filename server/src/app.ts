import { Hono } from "hono";
import { cors } from "hono/cors";
import { z } from "zod";
import { compilePolicy } from "./compiler.js";
import type { GenerateJson } from "./gemini.js";
import { generateInsight, WeeklyStatsSchema } from "./insights.js";
import { PresentPolicySchema } from "./policy.js";

const CompileRequestSchema = z.object({
  raw_prompt: z.string().trim().min(1).max(2000),
  existing_policy: PresentPolicySchema.optional(),
});

export const createApp = (deps: { readonly generate: GenerateJson; readonly apiKey: string }) => {
  const app = new Hono();

  app.get("/healthz", (c) => c.json({ status: "ok" }));

  app.use(
    "/v1/*",
    cors({ origin: "*", allowMethods: ["POST", "OPTIONS"], allowHeaders: ["content-type", "x-present-key"] }),
  );

  app.use("/v1/*", async (c, next) => {
    if (c.req.header("x-present-key") !== deps.apiKey) {
      return c.json({ status: "error", message: "Missing or wrong API key" }, 401);
    }
    return next();
  });

  app.post("/v1/compile", async (c) => {
    const request = CompileRequestSchema.safeParse(await c.req.json().catch(() => undefined));
    if (!request.success) {
      return c.json({ status: "error", message: "Tell Present what you want in a few words." }, 400);
    }

    const result = await compilePolicy({
      rawPrompt: request.data.raw_prompt,
      existingPolicy: request.data.existing_policy,
      generate: deps.generate,
    });

    switch (result.kind) {
      case "compiled":
        return c.json({ status: "compiled", policy: result.policy }, 200);
      case "rejected":
        return c.json({ status: "rejected", reason: result.reason }, 422);
      case "failed":
        return c.json({ status: "error", message: result.message }, 502);
    }
  });

  app.post("/v1/insights", async (c) => {
    const stats = WeeklyStatsSchema.safeParse(await c.req.json().catch(() => undefined));
    if (!stats.success) {
      return c.json({ status: "error", message: "Weekly stats must be non-negative counts." }, 400);
    }

    const insight = await generateInsight({ stats: stats.data, generate: deps.generate });
    return insight === undefined
      ? c.json({ status: "error", message: "Gemini couldn't write your insight right now." }, 502)
      : c.json(insight, 200);
  });

  return app;
};
