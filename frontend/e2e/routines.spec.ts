import { test, expect, Page } from '@playwright/test';

const API = 'http://localhost:8222';
const uniqueEmail = () => `pw.${Date.now()}${Math.floor(Math.random() * 10000)}@fittracker.test`;

async function registerAndLogin(page: Page, request: any): Promise<void> {
  const email = uniqueEmail();
  const res = await request.post(`${API}/auth/register`, {
    data: { email, password: 'Passw0rd!', name: 'Rutinas PW' },
  });
  expect(res.status()).toBe(201);

  await page.goto('/login');
  await page.locator('#email').fill(email);
  await page.locator('#password').fill('Passw0rd!');
  await page.getByRole('button', { name: 'Iniciar Sesion' }).click();
  await expect(page).toHaveURL(/\/workout$/);
}

/** Crea una rutina con 1 ejercicio (buscado por alias ingles 'Press') y tipos por defecto. */
async function createRoutine(page: Page, name: string): Promise<void> {
  await page.goto('/routines');
  await page.getByRole('link', { name: 'Nueva' }).click();
  await expect(page).toHaveURL(/\/routines\/new$/);
  await expect(page.getByRole('heading', { name: 'Nueva Rutina' })).toBeVisible();

  await page.getByPlaceholder('Ej: Empuje A, Piernas 5x5...').fill(name);
  await page.getByRole('button', { name: 'Fuerza' }).click();
  await page.getByRole('button', { name: 'Empuje' }).click();

  await page.getByPlaceholder('Buscar ejercicio para agregar...').fill('Press');
  await expect(page.locator('.search-result').first()).toBeVisible({ timeout: 15000 });
  await page.locator('.search-result').first().click();
  await expect(page.locator('.exercise-row')).toHaveCount(1);

  await page.getByRole('button', { name: 'Crear rutina' }).click();
  await expect(page).toHaveURL(/\/routines$/);
  await expect(page.locator('.routine-card')).toHaveCount(1);
}

test.describe('Mis Rutinas', () => {
  test('crear una rutina desde el listado vacio', async ({ page, request }) => {
    await registerAndLogin(page, request);

    await page.getByRole('link', { name: 'Rutinas' }).click();
    await expect(page).toHaveURL(/\/routines$/);
    await expect(page.getByRole('heading', { name: 'Sin rutinas' })).toBeVisible();

    await createRoutine(page, 'Empuje A');

    await expect(page.locator('.routine-card__name')).toHaveText('Empuje A');
    await expect(page.getByText('1 ejercicios')).toBeVisible();
    await expect(page.getByText('Fuerza')).toBeVisible();
    await expect(page.getByText('Empuje', { exact: true }).first()).toBeVisible();
  });

  test('editar una rutina renombra la tarjeta', async ({ page, request }) => {
    await registerAndLogin(page, request);

    await createRoutine(page, 'Rutina Original');

    await page.getByRole('link', { name: 'Editar' }).click();
    await expect(page).toHaveURL(/\/routines\/[0-9a-f-]+\/edit$/);
    await expect(page.getByRole('heading', { name: 'Editar Rutina' })).toBeVisible();
    await expect(page.getByPlaceholder('Ej: Empuje A, Piernas 5x5...')).toHaveValue('Rutina Original');

    await page.getByPlaceholder('Ej: Empuje A, Piernas 5x5...').fill('Rutina Renombrada');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();

    await expect(page).toHaveURL(/\/routines$/);
    await expect(page.locator('.routine-card__name')).toHaveText('Rutina Renombrada');
  });

  test('entrenar desde una rutina pre-carga la sesion con sus ejercicios', async ({ page, request }) => {
    await registerAndLogin(page, request);

    await createRoutine(page, 'Push PW');

    await page.locator('.routine-card').getByRole('link', { name: 'Entrenar' }).click();
    await expect(page).toHaveURL(/\/workout\/start\?routine=[0-9a-f-]+$/);
    await expect(page.getByRole('heading', { name: 'Nuevo Entrenamiento' })).toBeVisible();

    // la rutina viene preseleccionada y arrastra tipo + grupo muscular
    await expect(page.locator('.routine-chip--active')).toContainText('Push PW');
    await expect(page.locator('.type-card--active')).toContainText('Fuerza');
    await expect(page.locator('.muscle-chip--active')).toHaveText('Empuje');

    await page.getByRole('button', { name: 'Iniciar Entrenamiento' }).click();
    await expect(page).toHaveURL(/\/workout\/session\/[0-9a-f-]+$/);

    // titulo de la sesion = nombre de la rutina y ejercicio ya copiado
    await expect(page.locator('.session-header h1')).toHaveText('Push PW');
    await expect(page.locator('.exercise-card')).toHaveCount(1);
  });

  test('agregar un ejercicio a la sesion en curso desde el catalogo', async ({ page, request }) => {
    await registerAndLogin(page, request);

    await createRoutine(page, 'Sesion Add');
    await page.locator('.routine-card').getByRole('link', { name: 'Entrenar' }).click();
    await page.getByRole('button', { name: 'Iniciar Entrenamiento' }).click();
    await expect(page.locator('.exercise-card')).toHaveCount(1);

    await page.getByRole('button', { name: 'Agregar ejercicio' }).click();
    await expect(page.getByRole('heading', { name: 'Agregar ejercicio' })).toBeVisible();

    await page.getByPlaceholder('Buscar ejercicio...').fill('Sentadilla');
    await expect(page.locator('.add-result').first()).toBeVisible({ timeout: 15000 });
    await page.locator('.add-result').first().click();

    await expect(page.locator('.exercise-card')).toHaveCount(2);
    await expect(page.getByRole('heading', { name: 'Agregar ejercicio' })).toHaveCount(0);
  });

  test('eliminar ejercicio con series registradas pide confirmacion', async ({ page, request }) => {
    await registerAndLogin(page, request);

    await createRoutine(page, 'Sesion Delete');
    await page.locator('.routine-card').getByRole('link', { name: 'Entrenar' }).click();
    await page.getByRole('button', { name: 'Iniciar Entrenamiento' }).click();
    await expect(page.locator('.exercise-card')).toHaveCount(1);

    // registrar una serie: kg x reps + check
    await page.locator('.exec-input--kg').first().fill('60');
    await page.locator('.exec-input--reps').first().fill('8');
    await page.locator('.set-row__check').first().click();
    await expect(page.locator('.set-row--completed')).toHaveCount(1);

    // eliminar sin confirmacion NO deberia pasar: hay datos, aparece el modal
    await page.locator('.exercise-card__actions .btn-icon--danger').first().click();
    const modal = page.locator('.modal-dialog');
    await expect(modal).toContainText('Seguro que deseas eliminar este ejercicio?');

    await modal.getByRole('button', { name: 'Cancelar' }).click();
    await expect(page.locator('.exercise-card')).toHaveCount(1);

    await page.locator('.exercise-card__actions .btn-icon--danger').first().click();
    await modal.getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.locator('.exercise-card')).toHaveCount(0);
  });

  test('eliminar una rutina pide confirmacion y vacia el listado', async ({ page, request }) => {
    await registerAndLogin(page, request);

    await createRoutine(page, 'Rutina Borrar');

    await page.getByRole('button', { name: 'Eliminar' }).click();
    const modal = page.locator('.modal-dialog');
    await expect(modal).toContainText('Rutina Borrar');

    await modal.getByRole('button', { name: 'Cancelar' }).click();
    await expect(page.locator('.routine-card')).toHaveCount(1);

    await page.getByRole('button', { name: 'Eliminar' }).click();
    await modal.getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.getByRole('heading', { name: 'Sin rutinas' })).toBeVisible();
  });
});
