import { describe, expect, it } from "vitest";
import type { PresentPolicy } from "../../../server/src/policy";
import { DEFAULT_POLICY, cssForPolicy, redirectFor, resolvePolicy, selectorsForPolicy } from "./firewall";

type Rule = PresentPolicy["rules"][number];

const at = (hhmm: string): Date => new Date(`2026-11-20T${hhmm}:00`);

const aRule = (overrides: Partial<Rule> = {}): Rule => ({
  platform: "youtube",
  target_surfaces: ["shorts_player"],
  allowed_surfaces: ["search", "subscriptions"],
  schedule: { mode: "always" },
  ...overrides,
});

const aPolicy = (rules: readonly Rule[]): PresentPolicy => ({
  policy_version: "1.0",
  summary: "test policy",
  rules: [...rules],
});

const youtubeShortsSelectors = [
  "ytd-rich-shelf-renderer[is-shorts]",
  "ytd-reel-shelf-renderer",
  'a[title="Shorts"]',
  'ytd-guide-entry-renderer:has(a[title="Shorts"])',
  'ytd-mini-guide-entry-renderer[aria-label="Shorts"]',
  "#shorts-container",
  "ytd-rich-section-renderer:has(ytd-rich-shelf-renderer[is-shorts])",
];

describe("cssForPolicy", () => {
  it("hides every YouTube Shorts surface when shorts_player is targeted", () => {
    const css = cssForPolicy({ policy: aPolicy([aRule()]), host: "www.youtube.com", now: at("12:00") });

    youtubeShortsSelectors.forEach((selector) => {
      expect(css).toContain(`${selector} { display: none !important; }`);
    });
  });

  it("hides the YouTube home feed only when home_feed is targeted", () => {
    const withHome = aPolicy([aRule({ target_surfaces: ["home_feed"] })]);
    const withoutHome = aPolicy([aRule()]);
    const homeFeed = 'ytd-browse[page-subtype="home"] #contents';

    expect(cssForPolicy({ policy: withHome, host: "www.youtube.com", now: at("12:00") })).toContain(homeFeed);
    expect(cssForPolicy({ policy: withoutHome, host: "www.youtube.com", now: at("12:00") })).not.toContain(homeFeed);
  });

  it("hides Instagram Reels and Explore entry points", () => {
    const policy = aPolicy([aRule({ platform: "instagram", target_surfaces: ["reels_tab", "explore_grid"] })]);

    const css = cssForPolicy({ policy, host: "www.instagram.com", now: at("12:00") });

    expect(css).toContain('a[href="/reels/"]');
    expect(css).toContain('a[href*="/reels/"]');
    expect(css).toContain('a[href="/explore/"]');
  });

  it("applies only the rules for the platform of the current host", () => {
    const policy = aPolicy([aRule({ platform: "instagram", target_surfaces: ["reels_tab"] })]);

    expect(cssForPolicy({ policy, host: "www.youtube.com", now: at("12:00") })).toBe("");
  });

  it("matches mobile and bare hosts", () => {
    const policy = aPolicy([aRule()]);

    expect(cssForPolicy({ policy, host: "m.youtube.com", now: at("12:00") })).toContain("#shorts-container");
    expect(cssForPolicy({ policy, host: "youtube.com", now: at("12:00") })).toContain("#shorts-container");
  });

  it("returns no CSS for unrelated hosts, including look-alike domains", () => {
    const policy = aPolicy([aRule()]);

    expect(cssForPolicy({ policy, host: "example.com", now: at("12:00") })).toBe("");
    expect(cssForPolicy({ policy, host: "notyoutube.com", now: at("12:00") })).toBe("");
  });

  it("returns no CSS for platforms the extension does not cover yet", () => {
    const policy = aPolicy([aRule({ platform: "tiktok", target_surfaces: ["home_feed"] })]);

    expect(cssForPolicy({ policy, host: "www.tiktok.com", now: at("12:00") })).toBe("");
  });

  it("does not repeat selectors when several rules target the same surface", () => {
    const policy = aPolicy([aRule(), aRule()]);

    const selectors = selectorsForPolicy({ policy, host: "www.youtube.com", now: at("12:00") });

    expect(selectors).toEqual([...new Set(selectors)]);
  });

  describe("schedules", () => {
    const windowed = (start_time: string, end_time: string): PresentPolicy =>
      aPolicy([aRule({ schedule: { mode: "time_window", start_time, end_time } })]);

    it("blocks inside a same-day time window", () => {
      expect(cssForPolicy({ policy: windowed("09:00", "17:00"), host: "www.youtube.com", now: at("09:00") })).not.toBe("");
      expect(cssForPolicy({ policy: windowed("09:00", "17:00"), host: "www.youtube.com", now: at("16:59") })).not.toBe("");
    });

    it("does not block outside a same-day time window", () => {
      expect(cssForPolicy({ policy: windowed("09:00", "17:00"), host: "www.youtube.com", now: at("08:59") })).toBe("");
      expect(cssForPolicy({ policy: windowed("09:00", "17:00"), host: "www.youtube.com", now: at("17:00") })).toBe("");
    });

    it("wraps overnight windows past midnight", () => {
      const overnight = windowed("22:00", "07:00");

      expect(cssForPolicy({ policy: overnight, host: "www.youtube.com", now: at("23:30") })).not.toBe("");
      expect(cssForPolicy({ policy: overnight, host: "www.youtube.com", now: at("06:59") })).not.toBe("");
      expect(cssForPolicy({ policy: overnight, host: "www.youtube.com", now: at("12:00") })).toBe("");
    });

    it("accepts single-digit hours", () => {
      expect(cssForPolicy({ policy: windowed("9:00", "17:00"), host: "www.youtube.com", now: at("08:30") })).toBe("");
      expect(cssForPolicy({ policy: windowed("9:00", "17:00"), host: "www.youtube.com", now: at("09:30") })).not.toBe("");
    });

    it("blocks all day when a time window is missing a bound", () => {
      const policy = aPolicy([aRule({ schedule: { mode: "time_window", start_time: "09:00" } })]);

      expect(cssForPolicy({ policy, host: "www.youtube.com", now: at("03:00") })).not.toBe("");
    });

    it("treats a daily allowance as always on", () => {
      const policy = aPolicy([aRule({ schedule: { mode: "daily_allowance", daily_limit_minutes: 15 } })]);

      expect(cssForPolicy({ policy, host: "www.youtube.com", now: at("03:00") })).not.toBe("");
    });
  });

  describe("utility surfaces stay usable", () => {
    const everything = aPolicy([
      aRule({ target_surfaces: ["reels_tab", "shorts_player", "home_feed", "explore_grid", "spotlight"] }),
      aRule({ platform: "instagram", target_surfaces: ["reels_tab", "shorts_player", "home_feed", "explore_grid", "spotlight"] }),
    ]);

    it.each(["www.youtube.com", "www.instagram.com"])("never hides search, subscriptions or DMs on %s", (host) => {
      const css = cssForPolicy({ policy: everything, host, now: at("12:00") }).toLowerCase();

      expect(css).not.toContain("search");
      expect(css).not.toContain("subscriptions");
      expect(css).not.toContain("/direct/");
      expect(css).not.toContain("/explore/tags/");
    });
  });
});

describe("DEFAULT_POLICY", () => {
  it("hides YouTube Shorts and Instagram Reels at any time", () => {
    expect(cssForPolicy({ policy: DEFAULT_POLICY, host: "www.youtube.com", now: at("03:00") })).toContain("#shorts-container");
    expect(cssForPolicy({ policy: DEFAULT_POLICY, host: "www.instagram.com", now: at("15:00") })).toContain('a[href="/reels/"]');
  });
});

describe("redirectFor", () => {
  const shortsPolicy = aPolicy([aRule()]);
  const reelsPolicy = aPolicy([aRule({ platform: "instagram", target_surfaces: ["reels_tab"] })]);

  it("sends a YouTube Short to the regular watch page", () => {
    expect(redirectFor({ policy: shortsPolicy, url: "https://www.youtube.com/shorts/abc123", now: at("12:00") })).toBe(
      "https://www.youtube.com/watch?v=abc123",
    );
  });

  it("keeps the YouTube host when redirecting from mobile", () => {
    expect(redirectFor({ policy: shortsPolicy, url: "https://m.youtube.com/shorts/abc123?feature=share", now: at("12:00") })).toBe(
      "https://m.youtube.com/watch?v=abc123",
    );
  });

  it("leaves Shorts alone when shorts_player is not targeted", () => {
    const policy = aPolicy([aRule({ target_surfaces: ["home_feed"] })]);

    expect(redirectFor({ policy, url: "https://www.youtube.com/shorts/abc123", now: at("12:00") })).toBeNull();
  });

  it("leaves Shorts alone outside the rule's time window", () => {
    const policy = aPolicy([aRule({ schedule: { mode: "time_window", start_time: "09:00", end_time: "17:00" } })]);

    expect(redirectFor({ policy, url: "https://www.youtube.com/shorts/abc123", now: at("20:00") })).toBeNull();
  });

  it("does not redirect ordinary YouTube pages", () => {
    expect(redirectFor({ policy: shortsPolicy, url: "https://www.youtube.com/watch?v=abc123", now: at("12:00") })).toBeNull();
    expect(redirectFor({ policy: shortsPolicy, url: "https://www.youtube.com/results?search_query=shorts", now: at("12:00") })).toBeNull();
  });

  it("sends the Instagram Reels feed to the DM inbox", () => {
    expect(redirectFor({ policy: reelsPolicy, url: "https://www.instagram.com/reels/", now: at("12:00") })).toBe(
      "https://www.instagram.com/direct/inbox/",
    );
    expect(redirectFor({ policy: reelsPolicy, url: "https://www.instagram.com/reels/xyz/", now: at("12:00") })).toBe(
      "https://www.instagram.com/direct/inbox/",
    );
  });

  it("does not redirect Instagram DMs or single shared reels", () => {
    expect(redirectFor({ policy: reelsPolicy, url: "https://www.instagram.com/direct/inbox/", now: at("12:00") })).toBeNull();
    expect(redirectFor({ policy: reelsPolicy, url: "https://www.instagram.com/reel/xyz/", now: at("12:00") })).toBeNull();
  });

  it("sends the Instagram Explore grid to the DM inbox but keeps search and tags", () => {
    const policy = aPolicy([aRule({ platform: "instagram", target_surfaces: ["explore_grid"] })]);

    expect(redirectFor({ policy, url: "https://www.instagram.com/explore/", now: at("12:00") })).toBe(
      "https://www.instagram.com/direct/inbox/",
    );
    expect(redirectFor({ policy, url: "https://www.instagram.com/explore/search/", now: at("12:00") })).toBeNull();
    expect(redirectFor({ policy, url: "https://www.instagram.com/explore/tags/cats/", now: at("12:00") })).toBeNull();
  });

  it("returns null for unparseable URLs", () => {
    expect(redirectFor({ policy: shortsPolicy, url: "not a url", now: at("12:00") })).toBeNull();
  });
});

describe("resolvePolicy", () => {
  it("uses a stored policy when it matches the schema", () => {
    const stored = aPolicy([aRule({ target_surfaces: ["home_feed"] })]);

    expect(resolvePolicy(stored)).toEqual(stored);
  });

  it("falls back to the default policy when nothing valid is stored", () => {
    expect(resolvePolicy(undefined)).toEqual(DEFAULT_POLICY);
    expect(resolvePolicy({ rules: "nope" })).toEqual(DEFAULT_POLICY);
  });
});
