import type { PresentPolicy } from "../../../server/src/policy";

type Schedule = PresentPolicy["rules"][number]["schedule"];

export const describeSchedule = (schedule: Schedule): string => {
  if (schedule.mode === "time_window") {
    return `Every day, ${schedule.start_time ?? "00:00"} to ${schedule.end_time ?? "24:00"}`;
  }
  if (schedule.mode === "daily_allowance") {
    return `${schedule.daily_limit_minutes ?? 0} min a day allowance (blocked all day on desktop)`;
  }
  return "Always on";
};

export const surfaceLabel = (surface: string): string => {
  const words = surface.replaceAll("_", " ");
  return words.charAt(0).toUpperCase() + words.slice(1);
};

type Platform = PresentPolicy["rules"][number]["platform"];

const PLATFORM_LABELS: Record<Platform, string> = {
  youtube: "YouTube",
  instagram: "Instagram",
  tiktok: "TikTok",
  snapchat: "Snapchat",
  reddit: "Reddit",
  linkedin: "LinkedIn",
};

export const platformLabel = (platform: Platform): string => PLATFORM_LABELS[platform];
