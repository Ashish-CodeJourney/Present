import { z } from "zod";

export type Settings = { readonly apiUrl: string; readonly apiKey: string };

export const DEFAULT_SETTINGS: Settings = { apiUrl: "http://localhost:8080", apiKey: "dev-key" };

const ApiUrl = z.url({ protocol: /^https?$/ });
const ApiKey = z.string().min(1);
const StoredSettings = z.object({ apiUrl: z.unknown(), apiKey: z.unknown() }).partial();

export const resolveSettings = (stored: unknown): Settings => {
  const fields = StoredSettings.safeParse(stored);
  const raw = fields.success ? fields.data : {};
  const apiUrl = ApiUrl.safeParse(raw.apiUrl);
  const apiKey = ApiKey.safeParse(raw.apiKey);
  return {
    apiUrl: apiUrl.success ? apiUrl.data : DEFAULT_SETTINGS.apiUrl,
    apiKey: apiKey.success ? apiKey.data : DEFAULT_SETTINGS.apiKey,
  };
};
