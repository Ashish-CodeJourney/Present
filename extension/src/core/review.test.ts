import { describe, expect, it } from "vitest";
import { describeSchedule, platformLabel, surfaceLabel } from "./review";

describe("describeSchedule", () => {
  it("describes an always-on rule", () => {
    expect(describeSchedule({ mode: "always" })).toBe("Always on");
  });

  it("describes a time window", () => {
    expect(describeSchedule({ mode: "time_window", start_time: "22:00", end_time: "07:00" })).toBe("Every day, 22:00 to 07:00");
  });

  it("describes a daily allowance", () => {
    expect(describeSchedule({ mode: "daily_allowance", daily_limit_minutes: 15 })).toBe("15 min a day allowance (blocked all day on desktop)");
  });
});

describe("surfaceLabel", () => {
  it("turns schema identifiers into readable labels", () => {
    expect(surfaceLabel("shorts_player")).toBe("Shorts player");
    expect(surfaceLabel("direct_messages")).toBe("Direct messages");
  });
});

describe("platformLabel", () => {
  it("uses each platform's own brand spelling", () => {
    expect(platformLabel("youtube")).toBe("YouTube");
    expect(platformLabel("tiktok")).toBe("TikTok");
    expect(platformLabel("linkedin")).toBe("LinkedIn");
    expect(platformLabel("instagram")).toBe("Instagram");
  });
});
