import { serve } from "@hono/node-server";
import { createApp } from "./app.js";
import { firstSuccessful, geminiGenerator } from "./gemini.js";

const requireEnv = (name: string): string => {
  const value = process.env[name];
  if (value === undefined || value === "") throw new Error(`${name} must be set`);
  return value;
};

const models = (process.env["GEMINI_MODELS"] ?? "gemini-3.1-flash-lite,gemini-3.5-flash")
  .split(",")
  .map((model) => model.trim())
  .filter((model) => model !== "");

const timeoutMs = Number(process.env["GEMINI_TIMEOUT_MS"] ?? "6000");

const app = createApp({
  generate: firstSuccessful(models.map((model) => geminiGenerator({ model, timeoutMs }))),
  apiKey: requireEnv("PRESENT_API_KEY"),
});

const port = Number(process.env["PORT"] ?? "8080");
const server = serve({ fetch: app.fetch, port }, (info) => console.log(`present-compiler listening on :${info.port}`));

process.on("SIGTERM", () => server.close());
