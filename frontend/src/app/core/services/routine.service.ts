import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RoutineRequest, RoutineResponse } from '../models/workout.model';

@Injectable({ providedIn: 'root' })
export class RoutineService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/workouts/routines`;

  constructor(private http: HttpClient) {}

  list(): Observable<RoutineResponse[]> {
    return this.http.get<RoutineResponse[]>(this.baseUrl);
  }

  getById(id: string): Observable<RoutineResponse> {
    return this.http.get<RoutineResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: RoutineRequest): Observable<RoutineResponse> {
    return this.http.post<RoutineResponse>(this.baseUrl, request);
  }

  update(id: string, request: RoutineRequest): Observable<RoutineResponse> {
    return this.http.put<RoutineResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
