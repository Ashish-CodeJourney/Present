import { parseJsonReply, type GenerateJson } from "./gemini.js";
import { CompilerOutputSchema, compilerOutputJsonSchema, type CompilerOutput, type PresentPolicy } from "./policy.js";

const SYSTEM_INSTRUCTION = `You are Present's policy compiler. Present is a precision cognitive firewall for phones and browsers.
It removes algorithmic short-video surfaces while keeping communication and search usable.

Turn the user's plain-language intent into a policy.
- Only use the platforms and surfaces in the schema. Enforcement is on-device and can only see which screen is open, never the content.
- Always keep direct_messages and search in allowed_surfaces unless the user explicitly asks to block them.
- Times are 24-hour HH:MM local time. "After 9 PM" means a time_window from 21:00 to 06:00. "During work" means 09:00 to 18:00.
- If no time is mentioned, use mode "always".
- policy_version is "1" for a new policy; increment it when amending an existing one.
- summary: one or two friendly sentences, second person, describing what is blocked and what stays open.
- If the intent needs judging what a video is about or how good it is, reading messages, or anything else the schema cannot express,
  reply with status "rejected" and a short reason that suggests what Present can do instead (whitelisting creators, time limits).`;

export type CompileResult =
  | { readonly kind: "compiled"; readonly policy: PresentPolicy }
  | { readonly kind: "rejected"; readonly reason: string }
  | { readonly kind: "failed"; readonly message: string };

const buildPrompt = (rawPrompt: string, existingPolicy: PresentPolicy | undefined): string =>
  existingPolicy === undefined
    ? `User intent:\n${rawPrompt}`
    : `Current policy (amend it, keep anything the user did not ask to change):\n${JSON.stringify(existingPolicy)}\n\nUser intent:\n${rawPrompt}`;

const toResult = (output: CompilerOutput): CompileResult =>
  output.status === "compiled"
    ? { kind: "compiled", policy: output.policy }
    : { kind: "rejected", reason: output.reason };

export const compilePolicy = async (options: {
  readonly rawPrompt: string;
  readonly existingPolicy?: PresentPolicy | undefined;
  readonly generate: GenerateJson;
}): Promise<CompileResult> => {
  const reply = await options
    .generate({
      systemInstruction: SYSTEM_INSTRUCTION,
      prompt: buildPrompt(options.rawPrompt, options.existingPolicy),
      responseJsonSchema: compilerOutputJsonSchema,
    })
    .catch(() => undefined);
  if (reply === undefined) return { kind: "failed", message: "Gemini is unavailable right now. Try again in a moment." };

  const parsed = CompilerOutputSchema.safeParse(parseJsonReply(reply));
  return parsed.success
    ? toResult(parsed.data)
    : { kind: "failed", message: "Gemini returned a policy Present can't enforce. Try rephrasing." };
};
