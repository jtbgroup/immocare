/**
 * Dev seed script — supports multiple datasets.
 *
 * Usage:
 *   npm run seed:dev                          → demo dataset (default)
 *   npm run seed:dev -- --dataset=demo        → demo dataset (explicit)
 *   npm run seed:dev -- --dataset=real        → real dataset (Saint-Gilles)
 *   npm run seed:dev -- --dry-run             → demo, no API calls
 *   npm run seed:dev -- --dataset=real --dry-run
 *
 * Target: http://localhost:8080/api/v1 (override with API_URL env var)
 *
 * Branch: develop
 */
export {};

// ─── Dataset selection ────────────────────────────────────────────────────────

const DATASET = (() => {
  const arg = process.argv.find((a) => a.startsWith("--dataset="));
  return arg ? arg.split("=")[1] : "demo";
})();

if (!["demo", "real"].includes(DATASET)) {
  console.error(
    `❌  Unknown dataset "${DATASET}". Use --dataset=demo or --dataset=real`,
  );
  process.exit(1);
}

const DRY_RUN = process.argv.includes("--dry-run");

// ─── Logger ───────────────────────────────────────────────────────────────────

function log(emoji: string, msg: string) {
  console.log(`${emoji}  ${msg}`);
}

// ─── Dispatch ─────────────────────────────────────────────────────────────────

async function main() {
  if (DATASET === "real") {
    const mod = await import("./seed-real");
    await mod.runReal({ dryRun: DRY_RUN });
  } else {
    const mod = await import("./seed-demo");
    await mod.runDemo({ dryRun: DRY_RUN });
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
