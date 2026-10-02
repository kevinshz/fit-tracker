import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExerciseService } from './exercise.service';

describe('ExerciseService', () => {
  let service: ExerciseService;
  let httpMock: HttpTestingController;
  const base = 'http://localhost:8222/api/v1/exercises';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ExerciseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list arma la paginacion y filtros opcionales', () => {
    service.list('CHEST', 'BARBELL', 1, 10).subscribe();

    const req = httpMock.expectOne(
      r => r.url === base && r.params.get('muscle') === 'CHEST' && r.params.get('equipment') === 'BARBELL'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ content: [], totalElements: 0 });
  });

  it('list sin filtros opcionales no incluye muscle ni equipment', () => {
    service.list().subscribe();

    const req = httpMock.expectOne(r => r.url === base);
    expect(req.request.params.has('muscle')).toBeFalse();
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ content: [], totalElements: 0 });
  });

  it('search consulta /search con el texto q', () => {
    service.search('banca', 0, 5).subscribe();

    const req = httpMock.expectOne(r => r.url === `${base}/search`);
    expect(req.request.params.get('q')).toBe('banca');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ content: [], totalElements: 0 });
  });

  it('getById construye la URL con el id', () => {
    service.getById('abc-123').subscribe();

    const req = httpMock.expectOne(`${base}/abc-123`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 'abc-123' });
  });

  it('getMetadata consulta /metadata', () => {
    service.getMetadata().subscribe(m => expect(m['muscles']).toBeDefined());

    const req = httpMock.expectOne(`${base}/metadata`);
    req.flush({ muscles: ['CHEST'], types: ['STRENGTH'] });
  });
});
