import { describe, expect, it } from "vitest";
import { interceptsToday, recordIntercept } from "./stats";

const on = (iso: string): Date => new Date(iso);

describe("intercept stats", () => {
  it("starts at zero when nothing has been recorded", () => {
    expect(interceptsToday({ stats: undefined, now: on("2026-11-20T10:00:00") })).toBe(0);
  });

  it("counts intercepts recorded today", () => {
    const first = recordIntercept({ stats: undefined, now: on("2026-11-20T10:00:00") });
    const second = recordIntercept({ stats: first, now: on("2026-11-20T11:00:00") });

    expect(interceptsToday({ stats: second, now: on("2026-11-20T23:59:00") })).toBe(2);
  });

  it("resets the count on a new local day", () => {
    const yesterday = recordIntercept({ stats: undefined, now: on("2026-11-19T23:00:00") });

    expect(interceptsToday({ stats: yesterday, now: on("2026-11-20T00:01:00") })).toBe(0);
    expect(interceptsToday({ stats: recordIntercept({ stats: yesterday, now: on("2026-11-20T00:01:00") }), now: on("2026-11-20T00:02:00") })).toBe(1);
  });

  it("ignores malformed stored values", () => {
    expect(interceptsToday({ stats: "garbage", now: on("2026-11-20T10:00:00") })).toBe(0);
    expect(recordIntercept({ stats: { day: 5 }, now: on("2026-11-20T10:00:00") })).toEqual({ day: "2026-11-20", count: 1 });
  });
});
