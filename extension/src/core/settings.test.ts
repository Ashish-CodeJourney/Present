import { describe, expect, it } from "vitest";
import { DEFAULT_SETTINGS, resolveSettings } from "./settings";

describe("resolveSettings", () => {
  it("defaults to the local dev server", () => {
    expect(resolveSettings(undefined)).toEqual({ apiUrl: "http://localhost:8080", apiKey: "dev-key" });
    expect(DEFAULT_SETTINGS).toEqual({ apiUrl: "http://localhost:8080", apiKey: "dev-key" });
  });

  it("keeps stored values", () => {
    expect(resolveSettings({ apiUrl: "https://present-abc.a.run.app", apiKey: "secret" })).toEqual({
      apiUrl: "https://present-abc.a.run.app",
      apiKey: "secret",
    });
  });

  it("falls back per field when a stored value is blank or invalid", () => {
    expect(resolveSettings({ apiUrl: "", apiKey: "secret" })).toEqual({ apiUrl: "http://localhost:8080", apiKey: "secret" });
    expect(resolveSettings({ apiUrl: "not a url", apiKey: 42 })).toEqual(DEFAULT_SETTINGS);
  });
});
