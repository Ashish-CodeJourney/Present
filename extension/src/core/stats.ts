import { z } from "zod";

const InterceptStatsSchema = z.object({ day: z.string(), count: z.number().int().min(0) });

export type InterceptStats = z.infer<typeof InterceptStatsSchema>;

const pad = (value: number): string => String(value).padStart(2, "0");

const localDay = (now: Date): string => `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;

export const interceptsToday = ({ stats, now }: { stats: unknown; now: Date }): number => {
  const parsed = InterceptStatsSchema.safeParse(stats);
  return parsed.success && parsed.data.day === localDay(now) ? parsed.data.count : 0;
};

export const recordIntercept = ({ stats, now }: { stats: unknown; now: Date }): InterceptStats => ({
  day: localDay(now),
  count: interceptsToday({ stats, now }) + 1,
});
