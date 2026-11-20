import { serve } from "@hono/node-server";
import { createApp } from "./app.js";
import { geminiGenerator } from "./gemini.js";

const requireEnv = (name: string): string => {
  const value = process.env[name];
  if (value === undefined || value === "") throw new Error(`${name} must be set`);
  return value;
};

const app = createApp({
  generate: geminiGenerator({ model: process.env["GEMINI_MODEL"] ?? "gemini-2.5-flash" }),
  apiKey: requireEnv("PRESENT_API_KEY"),
});

const port = Number(process.env["PORT"] ?? "8080");
const server = serve({ fetch: app.fetch, port }, (info) => console.log(`present-compiler listening on :${info.port}`));

process.on("SIGTERM", () => server.close());
