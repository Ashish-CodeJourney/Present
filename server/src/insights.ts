import { z } from "zod";
import { parseJsonReply, type GenerateJson } from "./gemini.js";

const MINUTES_RECLAIMED_PER_INTERCEPT = 4.2;

const Count = z.number().int().min(0);

export const WeeklyStatsSchema = z.object({
  intercepts_by_app: z.record(z.string(), Count),
  streak_days: Count,
  total_intercepts: Count,
});

export type WeeklyStats = z.infer<typeof WeeklyStatsSchema>;

const InsightSchema = z.object({
  headline: z.string().describe("Under 8 words, celebratory"),
  insight: z.string().describe("Two sentences on what the numbers show"),
  suggestion: z.string().describe("One concrete rule the user could add to Present"),
});

export type Insight = z.infer<typeof InsightSchema>;

const insightJsonSchema = z.toJSONSchema(InsightSchema, { target: "draft-7" });

const SYSTEM_INSTRUCTION = `You are Present's weekly cognitive coach. Present blocks Reels, Shorts and other algorithmic feeds.
Write an encouraging, specific weekly report from aggregate counts. Never shame the user. Use plain words.`;

const minutesReclaimed = (stats: WeeklyStats): number =>
  Math.round(stats.total_intercepts * MINUTES_RECLAIMED_PER_INTERCEPT);

const buildPrompt = (stats: WeeklyStats): string =>
  [
    `Intercepts this week by app: ${JSON.stringify(stats.intercepts_by_app)}`,
    `Total intercepts: ${stats.total_intercepts}`,
    `Estimated time reclaimed: ${minutesReclaimed(stats)} minutes`,
    `Clean-day streak: ${stats.streak_days} days`,
  ].join("\n");

export const generateInsight = async (options: {
  readonly stats: WeeklyStats;
  readonly generate: GenerateJson;
}): Promise<Insight | undefined> => {
  const reply = await options
    .generate({
      systemInstruction: SYSTEM_INSTRUCTION,
      prompt: buildPrompt(options.stats),
      responseJsonSchema: insightJsonSchema,
    })
    .catch(() => undefined);
  if (reply === undefined) return undefined;
  const parsed = InsightSchema.safeParse(parseJsonReply(reply));
  return parsed.success ? parsed.data : undefined;
};
