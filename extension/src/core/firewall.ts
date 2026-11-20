import { PresentPolicySchema } from "../../../server/src/policy";
import type { PresentPolicy } from "../../../server/src/policy";

export type { PresentPolicy };

type Rule = PresentPolicy["rules"][number];
type Schedule = Rule["schedule"];
type Platform = Rule["platform"];
type BlockedSurface = Rule["target_surfaces"][number];

type SurfaceSelectors = Partial<Record<BlockedSurface, readonly string[]>>;

const SELECTORS: Partial<Record<Platform, SurfaceSelectors>> = {
  youtube: {
    shorts_player: [
      "ytd-rich-shelf-renderer[is-shorts]",
      "ytd-reel-shelf-renderer",
      'a[title="Shorts"]',
      'ytd-guide-entry-renderer:has(a[title="Shorts"])',
      'ytd-mini-guide-entry-renderer[aria-label="Shorts"]',
      "#shorts-container",
      "ytd-rich-section-renderer:has(ytd-rich-shelf-renderer[is-shorts])",
    ],
    home_feed: ['ytd-browse[page-subtype="home"] #contents'],
  },
  instagram: {
    reels_tab: ['a[href="/reels/"]', 'a[href*="/reels/"]'],
    explore_grid: ['a[href="/explore/"]'],
  },
};

const PLATFORM_DOMAINS: readonly (readonly [Platform, string])[] = [
  ["youtube", "youtube.com"],
  ["instagram", "instagram.com"],
];

const INSTAGRAM_INBOX = "/direct/inbox/";

export const DEFAULT_POLICY: PresentPolicy = {
  policy_version: "1.0",
  summary: "Default: YouTube Shorts and Instagram Reels are hidden. Search, subscriptions and DMs stay open.",
  rules: [
    {
      platform: "youtube",
      target_surfaces: ["shorts_player"],
      allowed_surfaces: ["search", "subscriptions", "creator_channel"],
      schedule: { mode: "always" },
    },
    {
      platform: "instagram",
      target_surfaces: ["reels_tab"],
      allowed_surfaces: ["direct_messages", "search", "profile"],
      schedule: { mode: "always" },
    },
  ],
};

export const resolvePolicy = (stored: unknown): PresentPolicy => {
  const parsed = PresentPolicySchema.safeParse(stored);
  return parsed.success ? parsed.data : DEFAULT_POLICY;
};

const platformForHost = (host: string): Platform | null => {
  const match = PLATFORM_DOMAINS.find(([, domain]) => host === domain || host.endsWith(`.${domain}`));
  return match === undefined ? null : match[0];
};

const minutesOf = (clock: string): number => {
  const [hours = "0", minutes = "0"] = clock.split(":");
  return Number(hours) * 60 + Number(minutes);
};

const isInWindow = ({ start, end, now }: { start: number; end: number; now: number }): boolean => {
  if (start === end) return true;
  if (start < end) return now >= start && now < end;
  return now >= start || now < end;
};

const isScheduleActive = (schedule: Schedule, now: Date): boolean => {
  if (schedule.mode !== "time_window") return true;
  if (schedule.start_time === undefined || schedule.end_time === undefined) return true;
  return isInWindow({
    start: minutesOf(schedule.start_time),
    end: minutesOf(schedule.end_time),
    now: now.getHours() * 60 + now.getMinutes(),
  });
};

const activeSurfaces = ({ policy, platform, now }: { policy: PresentPolicy; platform: Platform; now: Date }): ReadonlySet<BlockedSurface> =>
  new Set(
    policy.rules
      .filter((rule) => rule.platform === platform && isScheduleActive(rule.schedule, now))
      .flatMap((rule) => rule.target_surfaces),
  );

type PolicyAt = { readonly policy: PresentPolicy; readonly host: string; readonly now: Date };

export const selectorsForPolicy = ({ policy, host, now }: PolicyAt): readonly string[] => {
  const platform = platformForHost(host);
  if (platform === null) return [];
  const surfaces = SELECTORS[platform] ?? {};
  const selectors = [...activeSurfaces({ policy, platform, now })].flatMap((surface) => surfaces[surface] ?? []);
  return [...new Set(selectors)];
};

export const cssForPolicy = (args: PolicyAt): string =>
  selectorsForPolicy(args)
    .map((selector) => `${selector} { display: none !important; }`)
    .join("\n");

const parseUrl = (url: string): URL | null => {
  try {
    return new URL(url);
  } catch {
    return null;
  }
};

type Redirector = (args: { url: URL; surfaces: ReadonlySet<BlockedSurface> }) => string | null;

const youtubeRedirect: Redirector = ({ url, surfaces }) => {
  const shortId = /^\/shorts\/([^/?#]+)/.exec(url.pathname)?.[1];
  if (shortId === undefined || !surfaces.has("shorts_player")) return null;
  return `${url.origin}/watch?v=${encodeURIComponent(shortId)}`;
};

const instagramRedirect: Redirector = ({ url, surfaces }) => {
  const isReelsFeed = url.pathname.startsWith("/reels/") && surfaces.has("reels_tab");
  const isExploreGrid = url.pathname === "/explore/" && surfaces.has("explore_grid");
  return isReelsFeed || isExploreGrid ? `${url.origin}${INSTAGRAM_INBOX}` : null;
};

const REDIRECTORS: Partial<Record<Platform, Redirector>> = {
  youtube: youtubeRedirect,
  instagram: instagramRedirect,
};

export const redirectFor = ({ policy, url, now }: { policy: PresentPolicy; url: string; now: Date }): string | null => {
  const parsed = parseUrl(url);
  if (parsed === null) return null;
  const platform = platformForHost(parsed.hostname);
  if (platform === null) return null;
  const redirector = REDIRECTORS[platform];
  if (redirector === undefined) return null;
  return redirector({ url: parsed, surfaces: activeSurfaces({ policy, platform, now }) });
};
