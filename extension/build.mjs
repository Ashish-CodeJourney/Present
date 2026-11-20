import { build } from "esbuild";
import { copyFile, mkdir, rm } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL(".", import.meta.url));
const outdir = `${root}dist`;

const zodFromExtension = {
  name: "zod-from-extension",
  setup(pluginBuild) {
    pluginBuild.onResolve({ filter: /^zod(\/.*)?$/ }, async (args) => {
      if (args.pluginData === "zod-from-extension") return undefined;
      const result = await pluginBuild.resolve(args.path, {
        kind: args.kind,
        resolveDir: root,
        pluginData: "zod-from-extension",
      });
      return { path: result.path, errors: result.errors };
    });
  },
};

await rm(outdir, { recursive: true, force: true });
await mkdir(outdir, { recursive: true });

const result = await build({
  entryPoints: { content: `${root}src/content.ts`, popup: `${root}src/popup.ts` },
  outdir,
  bundle: true,
  format: "iife",
  target: "chrome114",
  minify: true,
  sourcemap: false,
  metafile: true,
  logLevel: "info",
  plugins: [zodFromExtension],
});

await Promise.all(
  ["manifest.json", "popup.html", "popup.css"].map((file) => copyFile(`${root}static/${file}`, `${outdir}/${file}`)),
);

const zodSources = new Set(Object.keys(result.metafile.inputs).filter((input) => input.includes("node_modules/zod/")).map((input) => input.split("/zod/")[0]));
console.log(`zod bundled from: ${[...zodSources].join(", ")}`);
console.log(`Present extension built into ${outdir}`);
