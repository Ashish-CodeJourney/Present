import { parseJsonReply, type GenerateJson } from "./gemini.js";
import { CompilerOutputSchema, compilerOutputJsonSchema, type CompilerOutput, type PresentPolicy } from "./policy.js";

const SYSTEM_INSTRUCTION = `You are Present's policy compiler. Present is a precision cognitive firewall for phones and browsers.
It removes algorithmic short-video surfaces while keeping communication and search usable.

Turn the user's plain-language intent into a policy.
- Only use the platforms and surfaces in the schema. Enforcement is on-device and can only see which screen is open, never the content.
- Present never blocks direct messages, search, subscriptions or profiles; they can only be kept. Always keep direct_messages
  and search in allowed_surfaces, even when the user asks to block them.
- Times are 24-hour HH:MM local time. "After 9 PM" means a time_window from 21:00 to 06:00. "During work" means 09:00 to 18:00.
- If no time is mentioned, use mode "always".
- policy_version is "1" for a new policy; increment it when amending an existing one.
- summary: one or two friendly sentences, second person, describing what is blocked and what stays open.
- If only part of the intent can be enforced, compile that part and end the summary with one short sentence saying what
  Present can't do, e.g. "DMs can't be blocked; Present always keeps your messages."
- Reject only when nothing in the intent can be enforced: for example it needs judging what a video is about or how good it is,
  reading messages, or only blocking things Present keeps. Then reply with status "rejected" and a short reason that suggests
  what Present can do instead (blocking feeds, schedules, whitelisting creators).`;

export type CompileResult =
  | { readonly kind: "compiled"; readonly policy: PresentPolicy }
  | { readonly kind: "rejected"; readonly reason: string }
  | { readonly kind: "failed"; readonly message: string };

const buildPrompt = (rawPrompt: string, existingPolicy: PresentPolicy | undefined): string =>
  existingPolicy === undefined
    ? `User intent:\n${rawPrompt}`
    : `Current policy (amend it, keep anything the user did not ask to change):\n${JSON.stringify(existingPolicy)}\n\nUser intent:\n${rawPrompt}`;

const NOTHING_TO_BLOCK =
  "There's nothing for Present to block there. It always keeps DMs, search, subscriptions and profiles. Try blocking Reels, Shorts, Spotlight or a feed instead.";

const blocksSomething = (policy: PresentPolicy): boolean =>
  policy.rules.some((rule) => rule.target_surfaces.length > 0);

const toResult = (output: CompilerOutput): CompileResult => {
  if (output.status === "rejected") return { kind: "rejected", reason: output.reason };
  return blocksSomething(output.policy)
    ? { kind: "compiled", policy: output.policy }
    : { kind: "rejected", reason: NOTHING_TO_BLOCK };
};

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
