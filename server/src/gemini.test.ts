import { describe, expect, it } from "vitest";
import { firstSuccessful, type GenerateJson, type GenerateRequest } from "./gemini.js";

const request: GenerateRequest = { systemInstruction: "s", prompt: "p", responseJsonSchema: {} };

const failing: GenerateJson = async () => {
  throw new Error("503 high demand");
};
const replying = (text: string): GenerateJson => async () => text;

describe("firstSuccessful", () => {
  it("uses the first model's reply when it answers", async () => {
    expect(await firstSuccessful([replying("primary"), replying("backup")])(request)).toBe("primary");
  });

  it("falls back to the next model when one is overloaded", async () => {
    expect(await firstSuccessful([failing, replying("backup")])(request)).toBe("backup");
  });

  it("fails when every model fails", async () => {
    await expect(firstSuccessful([failing, failing])(request)).rejects.toThrow("503 high demand");
  });
});
