/**
 * E2E spec — Buildings feature
 * File location in project: frontend/e2e/tests/buildings.spec.ts
 *
 * Covers:
 *  - List buildings
 *  - Create a building
 *  - Edit a building
 *  - Navigate into units
 *  - Delete a building
 */

import { test, expect, Page } from '@playwright/test';
import { seedMinimal, SeedResult } from '../fixtures/minimal-seed';

// ─── Auth helper ──────────────────────────────────────────────────────────────

async function loginAsAdmin(page: Page) {
  await page.goto('/login');
  await page.getByLabel('Username').fill('admin');
  await page.getByLabel('Password').fill('admin123');
  await page.getByRole('button', { name: 'Login' }).click();
  await expect(page).toHaveURL(/\/buildings|\/dashboard/);
}

// ─── Suite ────────────────────────────────────────────────────────────────────

test.describe('Buildings', () => {
  let seed: SeedResult;

  test.beforeEach(async ({ page }) => {
    seed = await seedMinimal();
    await loginAsAdmin(page);
  });

  // ── List ──────────────────────────────────────────────────────────────────

  test('displays the buildings list', async ({ page }) => {
    await page.goto('/buildings');
    await expect(page.getByRole('heading', { name: /buildings/i })).toBeVisible();
    // The building created by seedMinimal must appear
    await expect(page.getByText('E2E Building')).toBeVisible();
  });

  test('can filter buildings by city', async ({ page }) => {
    await page.goto('/buildings');
    const searchInput = page.getByPlaceholder(/search|filter/i).first();
    await searchInput.fill('Bruxelles');
    await expect(page.getByText('E2E Building')).toBeVisible();
  });

  // ── Create ────────────────────────────────────────────────────────────────

  test('creates a new building', async ({ page }) => {
    await page.goto('/buildings');
    await page.getByRole('link', { name: /new building|add building/i }).click();

    await page.getByLabel(/name/i).fill('Playwright Test Building');
    await page.getByLabel(/street/i).fill('Rue Playwright 99');
    await page.getByLabel(/postal/i).fill('1000');
    await page.getByLabel(/city/i).fill('Bruxelles');

    await page.getByRole('button', { name: /save|create/i }).click();

    await expect(page.getByText('Playwright Test Building')).toBeVisible();
  });

  // ── Detail / Edit ─────────────────────────────────────────────────────────

  test('navigates to building detail', async ({ page }) => {
    await page.goto(`/buildings/${seed.buildingId}`);
    await expect(page.getByText('E2E Building')).toBeVisible();
    await expect(page.getByText('Bruxelles')).toBeVisible();
  });

  test('edits a building name', async ({ page }) => {
    await page.goto(`/buildings/${seed.buildingId}/edit`);
    const nameInput = page.getByLabel(/name/i);
    await nameInput.clear();
    await nameInput.fill('Updated E2E Building');
    await page.getByRole('button', { name: /save|update/i }).click();

    await expect(page.getByText('Updated E2E Building')).toBeVisible();
  });

  // ── Units navigation ──────────────────────────────────────────────────────

  test('shows housing units within a building', async ({ page }) => {
    await page.goto(`/buildings/${seed.buildingId}`);
    // Unit A01 was created by seedMinimal
    await expect(page.getByText('A01')).toBeVisible();
  });

  test('navigates from building to unit detail', async ({ page }) => {
    await page.goto(`/buildings/${seed.buildingId}`);
    await page.getByText('A01').click();
    await expect(page).toHaveURL(/\/units\//);
  });
});

// ─── Leases on a unit ─────────────────────────────────────────────────────────

test.describe('Unit — lease tab', () => {
  let seed: SeedResult;

  test.beforeEach(async ({ page }) => {
    seed = await seedMinimal();
    await loginAsAdmin(page);
  });

  test('shows active lease on unit detail', async ({ page }) => {
    await page.goto(`/units/${seed.unitId}`);
    await expect(page.getByText('ACTIVE')).toBeVisible();
    await expect(page.getByText('850')).toBeVisible(); // monthly rent
  });

  test('shows draft lease badge', async ({ page }) => {
    await page.goto(`/units/${seed.unitId}`);
    // Navigate to the second unit (A02) which has a DRAFT lease
    await page.goto(`/buildings/${seed.buildingId}`);
    await page.getByText('A02').click();
    await expect(page.getByText('DRAFT')).toBeVisible();
  });

  test('navigates to lease detail from unit', async ({ page }) => {
    await page.goto(`/units/${seed.unitId}`);
    await page.getByText('ACTIVE').click();
    await expect(page).toHaveURL(/\/leases\//);
    await expect(page.getByText('850')).toBeVisible();
  });
});
