import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WorkoutService } from './workout.service';

describe('WorkoutService', () => {
  let service: WorkoutService;
  let httpMock: HttpTestingController;
  const base = 'http://localhost:8222/api/v1/workouts';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WorkoutService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('startSession hace POST a /sessions', () => {
    const request = { performedAt: '2026-09-28', muscleLabel: 'PUSH', sessionType: 'HYPERTROPHY' };
    service.startSession(request as any).subscribe();

    const req = httpMock.expectOne(`${base}/sessions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 's-1' });
  });

  it('logSet hace POST al set de la sesion', () => {
    service.logSet('s-1', { exerciseId: 'e-1', exerciseName: 'Press', setNumber: 1, weightKg: 80, reps: 8 } as any)
      .subscribe();

    const req = httpMock.expectOne(`${base}/sessions/s-1/sets`);
    expect(req.request.method).toBe('POST');
    req.flush({ volumeKg: 640 });
  });

  it('getSessions solo envia los params presentes', () => {
    service.getSessions('2026-01-01').subscribe();

    const req = httpMock.expectOne(r => r.url === `${base}/sessions`);
    expect(req.request.params.get('from')).toBe('2026-01-01');
    expect(req.request.params.has('to')).toBeFalse();
    req.flush([]);
  });

  it('deleteSession hace DELETE con el id', () => {
    service.deleteSession('s-9').subscribe();

    const req = httpMock.expectOne(`${base}/sessions/s-9`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('getVolumeReport envia from, to y groupBy', () => {
    service.getVolumeReport('2026-01-01', '2026-02-01', 'session').subscribe();

    const req = httpMock.expectOne(r => r.url === `${base}/volume/report`);
    expect(req.request.params.get('from')).toBe('2026-01-01');
    expect(req.request.params.get('to')).toBe('2026-02-01');
    expect(req.request.params.get('groupBy')).toBe('session');
    req.flush({ from: '', to: '', items: [] });
  });

  it('getVolumeReport usa groupBy=muscle por defecto', () => {
    service.getVolumeReport('2026-01-01', '2026-02-01').subscribe();

    const req = httpMock.expectOne(r => r.url === `${base}/volume/report`);
    expect(req.request.params.get('groupBy')).toBe('muscle');
    req.flush({ from: '', to: '', items: [] });
  });

  it('getProgressionRecommendation envia muscleLabel', () => {
    service.getProgressionRecommendation('PUSH' as any).subscribe();

    const req = httpMock.expectOne(r => r.url === `${base}/progression/recommendation`);
    expect(req.request.params.get('muscleLabel')).toBe('PUSH');
    req.flush({});
  });

  it('addSessionExercise hace POST con plannedSets por defecto 3', () => {
    service.addSessionExercise('s-1', 'e-7').subscribe();

    const req = httpMock.expectOne(`${base}/sessions/s-1/exercises`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ exerciseId: 'e-7', plannedSets: 3 });
    req.flush({ exerciseId: 'e-7', exerciseName: 'Press', position: 1, plannedSets: 3, sets: [] });
  });

  it('addSessionExercise respeta plannedSets explicito', () => {
    service.addSessionExercise('s-1', 'e-7', 5).subscribe();

    const req = httpMock.expectOne(`${base}/sessions/s-1/exercises`);
    expect(req.request.body).toEqual({ exerciseId: 'e-7', plannedSets: 5 });
    req.flush({ exerciseId: 'e-7', exerciseName: 'Press', position: 1, plannedSets: 5, sets: [] });
  });

  it('removeSessionExercise hace DELETE al ejercicio de la sesion', () => {
    service.removeSessionExercise('s-1', 'e-7').subscribe();

    const req = httpMock.expectOne(`${base}/sessions/s-1/exercises/e-7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
