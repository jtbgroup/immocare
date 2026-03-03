/**
 * Playwright configuration for ImmoCare E2E tests.
 * File location in project: frontend/playwright.config.ts
 *
 * Targets Angular dev server on localhost:4200.
 * Backend API must be running on localhost:8080.
 */

import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  // Directory where test files live
  testDir: './e2e/tests',

  // Fail the build on CI if test.only is left in
  forbidOnly: !!process.env['CI'],

  // Retry once on CI to absorb transient flakiness
  retries: process.env['CI'] ? 1 : 0,

  // Parallelism — reduce on CI to avoid DB contention
  workers: process.env['CI'] ? 2 : undefined,

  // HTML report (open with: npx playwright show-report)
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
  ],

  use: {
    // Angular dev server
    baseURL: process.env['BASE_URL'] ?? 'http://localhost:4200',

    // Keep traces on retry for debugging
    trace: 'on-first-retry',

    // Screenshot on failure
    screenshot: 'only-on-failure',

    // Video on retry
    video: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
  ],

  // Optional: start the Angular dev server automatically during local runs.
  // Comment this out if you prefer to start the server manually.
  // webServer: {
  //   command: 'npm start',
  //   url: 'http://localhost:4200',
  //   reuseExistingServer: true,
  //   timeout: 120_000,
  // },
});
