import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  const TOKEN_KEY = 'fittracker_token';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.removeItem(TOKEN_KEY);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem(TOKEN_KEY);
  });

  it('adjunta Authorization en rutas protegidas cuando hay token', () => {
    localStorage.setItem(TOKEN_KEY, 'token-valor');

    http.get('/api/v1/users/me').subscribe();

    const req = httpMock.expectOne('/api/v1/users/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-valor');
    req.flush({});
  });

  it('no adjunta Authorization en rutas /auth/', () => {
    localStorage.setItem(TOKEN_KEY, 'token-valor');

    http.post('/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('no adjunta Authorization sin token', () => {
    http.get('/api/v1/exercises').subscribe();

    const req = httpMock.expectOne('/api/v1/exercises');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
