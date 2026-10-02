import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ExerciseSummary, ExerciseResponse, PageResponse } from '../models/exercise.model';

@Injectable({ providedIn: 'root' })
export class ExerciseService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/exercises`;

  constructor(private http: HttpClient) {}

  list(muscle?: string, equipment?: string, page = 0, size = 20): Observable<PageResponse<ExerciseSummary>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (muscle) params = params.set('muscle', muscle);
    if (equipment) params = params.set('equipment', equipment);
    return this.http.get<PageResponse<ExerciseSummary>>(this.baseUrl, { params });
  }

  search(q: string, page = 0, size = 20): Observable<PageResponse<ExerciseSummary>> {
    const params = new HttpParams()
      .set('q', q)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<ExerciseSummary>>(`${this.baseUrl}/search`, { params });
  }

  getById(id: string): Observable<ExerciseResponse> {
    return this.http.get<ExerciseResponse>(`${this.baseUrl}/${id}`);
  }

  getMetadata(): Observable<Record<string, string[]>> {
    return this.http.get<Record<string, string[]>>(`${this.baseUrl}/metadata`);
  }
}
