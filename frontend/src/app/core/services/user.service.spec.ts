import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserService } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const base = 'http://localhost:8222/api/v1/users/me';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getProfile hace GET a /me', () => {
    service.getProfile().subscribe(p => expect(p.userId).toBe('u-1'));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush({ userId: 'u-1', name: 'Test' });
  });

  it('updateProfile hace PUT con el request', () => {
    const update = { bodyWeight: 74.2, height: 178.5 };
    service.updateProfile(update).subscribe(p => expect(p.bodyWeight).toBe(74.2));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(update);
    req.flush({ userId: 'u-1', ...update });
  });
});
