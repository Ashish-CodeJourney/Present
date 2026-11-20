import { describe, expect, it } from "vitest";
import type { PresentPolicy } from "../../../server/src/policy";
import { compilePolicy } from "./compile";

const aPolicy = (): PresentPolicy => ({
  policy_version: "1.0",
  summary: "Block Shorts, keep search.",
  rules: [
    {
      platform: "youtube",
      target_surfaces: ["shorts_player"],
      allowed_surfaces: ["search"],
      schedule: { mode: "always" },
    },
  ],
});

type Call = { readonly url: string; readonly init: RequestInit | undefined };

const fakeFetch = (status: number, body: unknown) => {
  const calls: Call[] = [];
  const fetchFn = async (url: string, init?: RequestInit): Promise<Response> => {
    calls.push({ url, init });
    return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
  };
  return { fetchFn, calls };
};

const settings = { apiUrl: "http://localhost:8080/", apiKey: "dev-key" };

describe("compilePolicy", () => {
  it("posts the prompt and key to the compile endpoint", async () => {
    const { fetchFn, calls } = fakeFetch(200, { status: "compiled", policy: aPolicy() });
    const existing = aPolicy();

    await compilePolicy({ ...settings, rawPrompt: "no shorts", existingPolicy: existing, fetchFn });

    expect(calls).toHaveLength(1);
    expect(calls[0]?.url).toBe("http://localhost:8080/v1/compile");
    expect(calls[0]?.init?.method).toBe("POST");
    expect(new Headers(calls[0]?.init?.headers).get("x-present-key")).toBe("dev-key");
    expect(JSON.parse(String(calls[0]?.init?.body))).toEqual({ raw_prompt: "no shorts", existing_policy: existing });
  });

  it("omits existing_policy when there is none", async () => {
    const { fetchFn, calls } = fakeFetch(200, { status: "compiled", policy: aPolicy() });

    await compilePolicy({ ...settings, rawPrompt: "no shorts", fetchFn });

    expect(JSON.parse(String(calls[0]?.init?.body))).toEqual({ raw_prompt: "no shorts" });
  });

  it("returns a validated policy on success", async () => {
    const { fetchFn } = fakeFetch(200, { status: "compiled", policy: aPolicy() });

    expect(await compilePolicy({ ...settings, rawPrompt: "no shorts", fetchFn })).toEqual({ kind: "compiled", policy: aPolicy() });
  });

  it("returns the rejection reason on 422", async () => {
    const { fetchFn } = fakeFetch(422, { status: "rejected", reason: "That is not about the web." });

    expect(await compilePolicy({ ...settings, rawPrompt: "bake a cake", fetchFn })).toEqual({
      kind: "rejected",
      reason: "That is not about the web.",
    });
  });

  it("returns the server's error message on other statuses", async () => {
    const { fetchFn } = fakeFetch(401, { status: "error", message: "bad key" });

    expect(await compilePolicy({ ...settings, rawPrompt: "x", fetchFn })).toEqual({ kind: "error", message: "bad key" });
  });

  it("rejects a compiled policy that does not match the schema", async () => {
    const { fetchFn } = fakeFetch(200, { status: "compiled", policy: { summary: "missing fields" } });

    const result = await compilePolicy({ ...settings, rawPrompt: "x", fetchFn });

    expect(result.kind).toBe("error");
  });

  it("reports network failures as errors", async () => {
    const fetchFn = async (): Promise<Response> => {
      throw new TypeError("Failed to fetch");
    };

    const result = await compilePolicy({ ...settings, rawPrompt: "x", fetchFn });

    expect(result).toEqual({ kind: "error", message: "Could not reach the Present service at http://localhost:8080/ (Failed to fetch)" });
  });
});
