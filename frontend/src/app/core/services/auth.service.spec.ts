import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

function fakeJwt(expSecondsFromNow: number): string {
  const payload = {
    sub: 'user-1',
    email: 'test@fittracker.test',
    roles: ['ROLE_USER'],
    iat: 1,
    exp: Math.floor(Date.now() / 1000) + expSecondsFromNow,
  };
  return 'header.' + btoa(JSON.stringify(payload)) + '.signature';
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const TOKEN_KEY = 'fittracker_token';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.removeItem(TOKEN_KEY);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem(TOKEN_KEY);
  });

  it('login guarda el token y lo publica en el estado', () => {
    let emitted: boolean | undefined;
    service.getAuthState().subscribe(v => (emitted = v));

    service.login({ email: 'a@b.c', password: 'x' }).subscribe(res => {
      expect(res.token).toBeTruthy();
    });

    const req = httpMock.expectOne('http://localhost:8222/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.c', password: 'x' });
    req.flush({ token: fakeJwt(3600), userId: 'user-1', email: 'a@b.c', roles: ['ROLE_USER'] });

    expect(localStorage.getItem(TOKEN_KEY)).toBeTruthy();
    expect(service.isLoggedIn()).toBeTrue();
    expect(emitted).toBeTrue();
  });

  it('register guarda la sesion igual que login', () => {
    service.register({ email: 'a@b.c', password: 'x', name: 'N' }).subscribe();

    const req = httpMock.expectOne('http://localhost:8222/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush({ token: fakeJwt(3600), userId: 'user-1', email: 'a@b.c', roles: ['ROLE_USER'] });

    expect(service.isLoggedIn()).toBeTrue();
  });

  it('isLoggedIn es false sin token', () => {
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getToken()).toBeNull();
    expect(service.getUser()).toBeNull();
  });

  it('isLoggedIn es false con token expirado', () => {
    localStorage.setItem(TOKEN_KEY, fakeJwt(-60));
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('isLoggedIn es false con token corrupto', () => {
    localStorage.setItem(TOKEN_KEY, 'esto.no-es-un-jwt');
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getUser()).toBeNull();
  });

  it('getUser decodifica el payload', () => {
    localStorage.setItem(TOKEN_KEY, fakeJwt(3600));
    const user = service.getUser();
    expect(user).not.toBeNull();
    expect(user!.sub).toBe('user-1');
    expect(user!.email).toBe('test@fittracker.test');
    expect(user!.roles).toEqual(['ROLE_USER']);
  });

  it('logout limpia token, emite false y navega a /login', () => {
    localStorage.setItem(TOKEN_KEY, fakeJwt(3600));
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate');
    let emitted: boolean | undefined = true;
    service.getAuthState().subscribe(v => (emitted = v));

    service.logout();

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
    expect(emitted).toBeFalse();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});
