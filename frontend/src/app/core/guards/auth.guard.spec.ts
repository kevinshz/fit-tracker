import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  function run(): boolean | UrlTree {
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    return result as boolean | UrlTree;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    localStorage.removeItem('fittracker_token');
  });

  it('permite el acceso con sesion valida', () => {
    const auth = TestBed.inject(AuthService);
    spyOn(auth, 'isLoggedIn').and.returnValue(true);

    expect(run()).toBeTrue();
  });

  it('redirige a /login sin sesion', () => {
    const auth = TestBed.inject(AuthService);
    spyOn(auth, 'isLoggedIn').and.returnValue(false);

    const result = run();

    expect(result).toBeInstanceOf(UrlTree);
    const router = TestBed.inject(Router);
    expect((result as UrlTree).toString()).toBe(router.parseUrl('/login').toString());
  });
});
