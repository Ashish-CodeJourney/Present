import { compilePolicy } from "./core/compile";
import { resolvePolicy } from "./core/firewall";
import type { PresentPolicy } from "./core/firewall";
import { describeSchedule, platformLabel, surfaceLabel } from "./core/review";
import { DEFAULT_SETTINGS, resolveSettings } from "./core/settings";
import type { Settings } from "./core/settings";
import { interceptsToday } from "./core/stats";
import { POLICY_KEY, SETTINGS_KEY, STATS_KEY } from "./storage-keys";

type ElementClass<T extends HTMLElement> = { new (): T; prototype: T };

const byId = <T extends HTMLElement>(id: string, kind: ElementClass<T>): T => {
  const element = document.getElementById(id);
  if (!(element instanceof kind)) throw new Error(`popup.html is missing #${id}`);
  return element;
};

const ui = {
  intercepts: byId("intercepts", HTMLSpanElement),
  currentSummary: byId("current-summary", HTMLParagraphElement),
  composeView: byId("compose-view", HTMLElement),
  prompt: byId("prompt", HTMLTextAreaElement),
  compile: byId("compile", HTMLButtonElement),
  rejected: byId("rejected", HTMLParagraphElement),
  error: byId("error", HTMLParagraphElement),
  reviewView: byId("review-view", HTMLElement),
  reviewSummary: byId("review-summary", HTMLParagraphElement),
  reviewRules: byId("review-rules", HTMLDivElement),
  discard: byId("discard", HTMLButtonElement),
  activate: byId("activate", HTMLButtonElement),
  activated: byId("activated", HTMLParagraphElement),
  apiUrl: byId("api-url", HTMLInputElement),
  apiKey: byId("api-key", HTMLInputElement),
  saveSettings: byId("save-settings", HTMLButtonElement),
  resetPolicy: byId("reset-policy", HTMLButtonElement),
};

const pending: { policy: PresentPolicy | null } = { policy: null };

const el = <K extends keyof HTMLElementTagNameMap>(
  tag: K,
  options: { readonly className?: string; readonly text?: string; readonly children?: readonly Node[] } = {},
): HTMLElementTagNameMap[K] => {
  const node = document.createElement(tag);
  if (options.className !== undefined) node.className = options.className;
  if (options.text !== undefined) node.textContent = options.text;
  node.append(...(options.children ?? []));
  return node;
};

const chips = (labels: readonly string[], kind: "blocked" | "allowed"): HTMLDivElement =>
  el("div", { className: "chips", children: labels.map((label) => el("span", { className: `chip ${kind}`, text: surfaceLabel(label) })) });

const ruleCard = (rule: PresentPolicy["rules"][number]): HTMLDivElement =>
  el("div", {
    className: "rule",
    children: [
      el("h2", { text: platformLabel(rule.platform) }),
      chips(rule.target_surfaces, "blocked"),
      chips(rule.allowed_surfaces, "allowed"),
      el("p", { className: "schedule", text: describeSchedule(rule.schedule) }),
    ],
  });

const show = (element: HTMLElement, visible: boolean): void => {
  element.hidden = !visible;
};

const setBusy = (busy: boolean): void => {
  ui.compile.disabled = busy;
  ui.compile.classList.toggle("busy", busy);
};

const showMessage = (target: HTMLParagraphElement, text: string | null): void => {
  target.textContent = text ?? "";
  show(target, text !== null);
};

const renderReview = (policy: PresentPolicy | null): void => {
  pending.policy = policy;
  show(ui.reviewView, policy !== null);
  show(ui.composeView, policy === null);
  if (policy === null) return;
  ui.reviewSummary.textContent = policy.summary;
  ui.reviewRules.replaceChildren(...policy.rules.map(ruleCard));
};

const readSettings = async (): Promise<Settings> => {
  const stored = await chrome.storage.local.get(SETTINGS_KEY);
  return resolveSettings(stored[SETTINGS_KEY]);
};

const readStoredPolicy = async (): Promise<PresentPolicy | undefined> => {
  const stored = await chrome.storage.sync.get(POLICY_KEY);
  return stored[POLICY_KEY] === undefined ? undefined : resolvePolicy(stored[POLICY_KEY]);
};

const refreshStatus = async (): Promise<void> => {
  const [stats, policy] = await Promise.all([chrome.storage.local.get(STATS_KEY), readStoredPolicy()]);
  ui.intercepts.textContent = String(interceptsToday({ stats: stats[STATS_KEY], now: new Date() }));
  ui.currentSummary.textContent = (policy ?? resolvePolicy(undefined)).summary;
};

const onCompile = async (): Promise<void> => {
  const rawPrompt = ui.prompt.value.trim();
  if (rawPrompt === "") {
    ui.prompt.focus();
    return;
  }
  showMessage(ui.rejected, null);
  showMessage(ui.error, null);
  show(ui.activated, false);
  setBusy(true);
  const [settings, existingPolicy] = await Promise.all([readSettings(), readStoredPolicy()]);
  const result = await compilePolicy({
    ...settings,
    rawPrompt,
    fetchFn: (url, init) => fetch(url, init),
    ...(existingPolicy === undefined ? {} : { existingPolicy }),
  });
  setBusy(false);
  if (result.kind === "compiled") return renderReview(result.policy);
  if (result.kind === "rejected") return showMessage(ui.rejected, result.reason);
  showMessage(ui.error, result.message);
};

const onActivate = async (): Promise<void> => {
  if (pending.policy === null) return;
  await chrome.storage.sync.set({ [POLICY_KEY]: pending.policy });
  renderReview(null);
  ui.prompt.value = "";
  show(ui.activated, true);
  await refreshStatus();
};

const onSaveSettings = async (): Promise<void> => {
  const settings = resolveSettings({ apiUrl: ui.apiUrl.value.trim(), apiKey: ui.apiKey.value.trim() });
  await chrome.storage.local.set({ [SETTINGS_KEY]: settings });
  ui.saveSettings.textContent = "Saved";
  setTimeout(() => {
    ui.saveSettings.textContent = "Save";
  }, 1200);
};

const onResetPolicy = async (): Promise<void> => {
  await chrome.storage.sync.remove(POLICY_KEY);
  await refreshStatus();
};

const init = async (): Promise<void> => {
  const settings = await readSettings();
  ui.apiUrl.value = settings.apiUrl;
  ui.apiKey.value = settings.apiKey;
  ui.apiUrl.placeholder = DEFAULT_SETTINGS.apiUrl;
  await refreshStatus();
};

ui.compile.addEventListener("click", () => void onCompile());
ui.activate.addEventListener("click", () => void onActivate());
ui.discard.addEventListener("click", () => renderReview(null));
ui.saveSettings.addEventListener("click", () => void onSaveSettings());
ui.resetPolicy.addEventListener("click", () => void onResetPolicy());
ui.prompt.addEventListener("keydown", (event) => {
  if (event.key === "Enter" && (event.metaKey || event.ctrlKey)) void onCompile();
});
chrome.storage.onChanged.addListener((_changes, area) => {
  if (area === "local" || area === "sync") void refreshStatus();
});

void init();
