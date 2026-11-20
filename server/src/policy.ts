import { z } from "zod";

const ClockTime = z
  .string()
  .regex(/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/)
  .describe("24-hour local time, HH:MM");

export const Platform = z.enum(["instagram", "youtube", "tiktok", "snapchat", "reddit", "linkedin"]);

export const BlockedSurface = z.enum(["reels_tab", "shorts_player", "home_feed", "explore_grid", "spotlight"]);

export const AllowedSurface = z.enum(["direct_messages", "search", "subscriptions", "profile", "creator_channel"]);

export const Schedule = z.object({
  mode: z.enum(["always", "time_window", "daily_allowance"]),
  start_time: ClockTime.optional(),
  end_time: ClockTime.optional(),
  daily_limit_minutes: z.number().int().min(0).max(1440).optional(),
});

export const Rule = z.object({
  platform: Platform,
  target_surfaces: z.array(BlockedSurface).describe("Algorithmic surfaces to block"),
  allowed_surfaces: z.array(AllowedSurface).describe("Utility surfaces that must stay usable"),
  schedule: Schedule,
  whitelisted_entities: z.array(z.string()).optional().describe("Creators or channels the user explicitly allows"),
});

export const PauseCardSettings = z.object({
  delay_seconds: z.number().int().min(0).max(120).optional(),
  prompt_reflection: z.boolean().optional(),
  side_quest_category: z.enum(["hydration", "physical", "mindfulness", "quick_learning"]).optional(),
});

export const PresentPolicySchema = z.object({
  policy_version: z.string(),
  summary: z.string().describe("One or two plain-language sentences the user will read before activating"),
  rules: z.array(Rule),
  pause_card_settings: PauseCardSettings.optional(),
});

export type PresentPolicy = z.infer<typeof PresentPolicySchema>;

export const CompilerOutputSchema = z.union([
  z.object({ status: z.enum(["compiled"]), policy: PresentPolicySchema }),
  z.object({ status: z.enum(["rejected"]), reason: z.string() }),
]);

export type CompilerOutput = z.infer<typeof CompilerOutputSchema>;

export const compilerOutputJsonSchema = z.toJSONSchema(CompilerOutputSchema, { target: "draft-7" });
