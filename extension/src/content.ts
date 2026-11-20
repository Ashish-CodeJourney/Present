import { cssForPolicy, redirectFor, resolvePolicy, selectorsForPolicy } from "./core/firewall";
import type { PresentPolicy } from "./core/firewall";
import { recordIntercept } from "./core/stats";
import { POLICY_KEY, STATS_KEY } from "./storage-keys";

const STYLE_ID = "present-firewall";
const RECOMPUTE_MS = 60_000;
const URL_POLL_MS = 500;
const SURFACE_CHECK_DEBOUNCE_MS = 400;

type State = {
  readonly policy: PresentPolicy;
  readonly countedUrl: string | null;
};

const createStore = (initial: State) => {
  const box = { current: initial };
  return {
    get: (): State => box.current,
    update: (next: Partial<State>): void => {
      box.current = { ...box.current, ...next };
    },
  };
};

const store = createStore({ policy: resolvePolicy(undefined), countedUrl: null });

const bumpIntercepts = async (): Promise<void> => {
  const stored = await chrome.storage.local.get(STATS_KEY);
  await chrome.storage.local.set({ [STATS_KEY]: recordIntercept({ stats: stored[STATS_KEY], now: new Date() }) });
};

const styleElement = (): HTMLStyleElement => {
  const existing = document.getElementById(STYLE_ID);
  if (existing instanceof HTMLStyleElement) return existing;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  (document.head ?? document.documentElement).appendChild(style);
  return style;
};

const applyStyle = (): void => {
  const css = cssForPolicy({ policy: store.get().policy, host: location.hostname, now: new Date() });
  const style = styleElement();
  if (style.textContent !== css) style.textContent = css;
};

const applyRedirect = (): boolean => {
  const target = redirectFor({ policy: store.get().policy, url: location.href, now: new Date() });
  if (target === null) return false;
  void bumpIntercepts();
  location.replace(target);
  return true;
};

const countVisibleSurface = (): void => {
  const href = location.href;
  if (store.get().countedUrl === href) return;
  const selectors = selectorsForPolicy({ policy: store.get().policy, host: location.hostname, now: new Date() });
  const blockedSomething = selectors.some((selector) => document.querySelector(selector) !== null);
  if (!blockedSomething) return;
  store.update({ countedUrl: href });
  void bumpIntercepts();
};

const debounce = (fn: () => void, ms: number): (() => void) => {
  const timer: { id: ReturnType<typeof setTimeout> | undefined } = { id: undefined };
  return () => {
    clearTimeout(timer.id);
    timer.id = setTimeout(fn, ms);
  };
};

const enforce = (): void => {
  applyStyle();
  if (applyRedirect()) return;
  countVisibleSurface();
};

const loadPolicy = async (): Promise<void> => {
  const stored = await chrome.storage.sync.get(POLICY_KEY);
  store.update({ policy: resolvePolicy(stored[POLICY_KEY]) });
  enforce();
};

const watchUrl = (onChange: () => void): void => {
  const last = { href: location.href };
  const check = (): void => {
    if (location.href === last.href) return;
    last.href = location.href;
    onChange();
  };
  setInterval(check, URL_POLL_MS);
  window.addEventListener("popstate", check);
  document.addEventListener("yt-navigate-finish", check);
};

const watchDom = (onMutate: () => void): void => {
  const observer = new MutationObserver(onMutate);
  const start = (): void => observer.observe(document.documentElement, { childList: true, subtree: true });
  if (document.documentElement) start();
  else document.addEventListener("DOMContentLoaded", start, { once: true });
};

applyStyle();
void loadPolicy();

chrome.storage.onChanged.addListener((changes, area) => {
  if (area === "sync" && POLICY_KEY in changes) void loadPolicy();
});

setInterval(enforce, RECOMPUTE_MS);
watchUrl(enforce);
watchDom(
  debounce(() => {
    applyStyle();
    countVisibleSurface();
  }, SURFACE_CHECK_DEBOUNCE_MS),
);
