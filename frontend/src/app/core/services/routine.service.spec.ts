import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RoutineService } from './routine.service';
import { RoutineRequest, RoutineResponse } from '../models/workout.model';

describe('RoutineService', () => {
  let service: RoutineService;
  let httpMock: HttpTestingController;
  const base = 'http://localhost:8222/api/v1/workouts/routines';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RoutineService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list hace GET a /routines', () => {
    service.list().subscribe();

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getById hace GET al recurso con id', () => {
    service.getById('r-1').subscribe();

    const req = httpMock.expectOne(`${base}/r-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 'r-1' } as RoutineResponse);
  });

  it('create hace POST con el body', () => {
    const body = { name: 'Push A', sessionType: 'HYPERTROPHY', muscleLabel: 'PUSH', exercises: [], notes: null } as RoutineRequest;
    service.create(body).subscribe();

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 'r-2' } as RoutineResponse);
  });

  it('update hace PUT al recurso con id', () => {
    const body = { name: 'Push A v2', sessionType: 'STRENGTH', muscleLabel: 'PUSH', exercises: [], notes: null } as RoutineRequest;
    service.update('r-1', body).subscribe();

    const req = httpMock.expectOne(`${base}/r-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 'r-1' } as RoutineResponse);
  });

  it('delete hace DELETE al recurso con id', () => {
    service.delete('r-3').subscribe();

    const req = httpMock.expectOne(`${base}/r-3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('usa el base url del gateway', () => {
    service.list().subscribe();

    const req = httpMock.expectOne(base);
    expect(req.request.url).toContain('http://localhost:8222');
    expect(req.request.url).toContain('/api/v1/workouts/routines');
    req.flush([]);
  });
});
