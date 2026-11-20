import { z } from "zod";
import { CompilerOutputSchema } from "../../../server/src/policy";
import type { PresentPolicy } from "../../../server/src/policy";

export type CompileResult =
  | { readonly kind: "compiled"; readonly policy: PresentPolicy }
  | { readonly kind: "rejected"; readonly reason: string }
  | { readonly kind: "error"; readonly message: string };

export type FetchFn = (url: string, init?: RequestInit) => Promise<Response>;

type CompileRequest = {
  readonly apiUrl: string;
  readonly apiKey: string;
  readonly rawPrompt: string;
  readonly existingPolicy?: PresentPolicy;
  readonly fetchFn: FetchFn;
};

const ServerErrorSchema = z.object({ status: z.literal("error"), message: z.string() });

const endpoint = (apiUrl: string): string => `${apiUrl.replace(/\/+$/, "")}/v1/compile`;

const readJson = async (response: Response): Promise<unknown> => {
  try {
    return await response.json();
  } catch {
    return undefined;
  }
};

const interpret = ({ httpStatus, body }: { httpStatus: number; body: unknown }): CompileResult => {
  const output = CompilerOutputSchema.safeParse(body);
  if (output.success && output.data.status === "compiled" && httpStatus === 200) {
    return { kind: "compiled", policy: output.data.policy };
  }
  if (output.success && output.data.status === "rejected") {
    return { kind: "rejected", reason: output.data.reason };
  }
  const serverError = ServerErrorSchema.safeParse(body);
  if (serverError.success) return { kind: "error", message: serverError.data.message };
  return { kind: "error", message: `Unexpected response from the Present service (HTTP ${httpStatus})` };
};

export const compilePolicy = async ({ apiUrl, apiKey, rawPrompt, existingPolicy, fetchFn }: CompileRequest): Promise<CompileResult> => {
  const body = existingPolicy === undefined ? { raw_prompt: rawPrompt } : { raw_prompt: rawPrompt, existing_policy: existingPolicy };
  try {
    const response = await fetchFn(endpoint(apiUrl), {
      method: "POST",
      headers: { "content-type": "application/json", "x-present-key": apiKey },
      body: JSON.stringify(body),
    });
    return interpret({ httpStatus: response.status, body: await readJson(response) });
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    return { kind: "error", message: `Could not reach the Present service at ${apiUrl} (${reason})` };
  }
};
