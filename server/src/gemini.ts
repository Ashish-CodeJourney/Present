import { GoogleGenAI } from "@google/genai";

export type GenerateRequest = {
  readonly systemInstruction: string;
  readonly prompt: string;
  readonly responseJsonSchema: unknown;
};

export type GenerateJson = (request: GenerateRequest) => Promise<string>;

export const parseJsonReply = (text: string): unknown => {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
};

export const geminiGenerator = (options: { readonly model: string }): GenerateJson => {
  const ai = new GoogleGenAI({});
  return async (request) => {
    const response = await ai.models.generateContent({
      model: options.model,
      contents: request.prompt,
      config: {
        systemInstruction: request.systemInstruction,
        responseMimeType: "application/json",
        responseJsonSchema: request.responseJsonSchema,
        temperature: 0.2,
        thinkingConfig: { thinkingBudget: 0 },
      },
    });
    return response.text ?? "";
  };
};
