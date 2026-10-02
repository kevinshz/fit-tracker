import { test, expect } from '@playwright/test';

const API = 'http://localhost:8222';
const uniqueEmail = () => `pw.${Date.now()}${Math.floor(Math.random() * 10000)}@fittracker.test`;

async function registerAndLogin(page: any, request: any): Promise<void> {
  const email = uniqueEmail();
  const res = await request.post(`${API}/auth/register`, {
    data: { email, password: 'Passw0rd!', name: 'Workout PW' },
  });
  expect(res.status()).toBe(201);

  await page.goto('/login');
  await page.locator('#email').fill(email);
  await page.locator('#password').fill('Passw0rd!');
  await page.getByRole('button', { name: 'Iniciar Sesion' }).click();
  await expect(page).toHaveURL(/\/workout$/);
}

test.describe('Entrenamiento', () => {
  test('flujo completo: crear sesion y verla en el historial', async ({ page, request }) => {
    await registerAndLogin(page, request);

    // estado vacio inicial
    await expect(page.getByRole('heading', { name: 'Sin entrenamientos' })).toBeVisible();

    await page.getByRole('link', { name: /Nuevo|Empezar/ }).first().click();
    await expect(page).toHaveURL(/\/workout\/start$/);
    await expect(page.getByRole('heading', { name: 'Nuevo Entrenamiento' })).toBeVisible();

    await page.getByRole('button', { name: 'Fuerza' }).click();
    await page.getByRole('button', { name: 'Empuje' }).click();
    await page.getByRole('button', { name: 'Iniciar Entrenamiento' }).click();

    await expect(page).toHaveURL(/\/workout\/session\/[0-9a-f-]+$/);

    // volver al historial: la sesion ya no debe mostrar el estado vacio
    await page.goto('/workout');
    await expect(page.getByRole('heading', { name: 'Sin entrenamientos' })).toHaveCount(0);
    await expect(page.locator('.session-card').first()).toBeVisible();
  });

  test('listado de ejercicios carga el catalogo', async ({ page, request }) => {
    await registerAndLogin(page, request);

    await page.goto('/exercises');
    await expect(page.getByRole('heading', { name: 'Ejercicios' })).toBeVisible();
    await expect(page.locator('.exercise-card').first()).toBeVisible({ timeout: 20000 });

    await page.getByPlaceholder('Buscar ejercicio...').fill('press');
    await expect(page.locator('.exercise-card').first()).toBeVisible();
  });
});
