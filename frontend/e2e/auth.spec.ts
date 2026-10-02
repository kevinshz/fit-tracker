import { test, expect } from '@playwright/test';

const API = 'http://localhost:8222';
const uniqueEmail = () => `pw.${Date.now()}${Math.floor(Math.random() * 10000)}@fittracker.test`;

test.describe('Autenticacion', () => {
  test('el guard redirige a /login sin sesion', async ({ page }) => {
    await page.goto('/workout');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'FitTracker' })).toBeVisible();
  });

  test('registro crea sesion y entra al historial', async ({ page }) => {
    await page.goto('/register');
    await page.locator('input[formcontrolname="name"]').fill('Usuario Playwright');
    await page.locator('input[formcontrolname="email"]').fill(uniqueEmail());
    await page.locator('input[formcontrolname="password"]').fill('Passw0rd!');
    await page.locator('input[formcontrolname="confirmPassword"]').fill('Passw0rd!');
    await page.getByRole('button', { name: 'Crear Cuenta' }).click();

    await expect(page).toHaveURL(/\/workout$/);
    await expect(page.getByRole('heading', { name: 'Mis Entrenamientos' })).toBeVisible();
  });

  test('login con credenciales incorrectas muestra error', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#email').fill(uniqueEmail());
    await page.locator('#password').fill('clave-mala-123');
    await page.getByRole('button', { name: 'Iniciar Sesion' }).click();

    await expect(page.locator('.alert-danger')).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('login con credenciales correctas entra al historial', async ({ page, request }) => {
    const email = uniqueEmail();
    const register = await request.post(`${API}/auth/register`, {
      data: { email, password: 'Passw0rd!', name: 'Login PW' },
    });
    expect(register.status()).toBe(201);

    await page.goto('/login');
    await page.locator('#email').fill(email);
    await page.locator('#password').fill('Passw0rd!');
    await page.getByRole('button', { name: 'Iniciar Sesion' }).click();

    await expect(page).toHaveURL(/\/workout$/);
    await expect(page.getByRole('heading', { name: 'Mis Entrenamientos' })).toBeVisible();
  });
});
