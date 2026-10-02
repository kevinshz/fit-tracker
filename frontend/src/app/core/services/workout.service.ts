import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  StartSessionRequest, LogSetRequest, SessionResponse, SessionExerciseDto,
  VolumeReportDto, ProgressionRecommendation
} from '../models/workout.model';
import { MuscleLabel } from '../models/enums';

@Injectable({ providedIn: 'root' })
export class WorkoutService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/workouts`;

  constructor(private http: HttpClient) {}

  startSession(request: StartSessionRequest): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(`${this.baseUrl}/sessions`, request);
  }

  getSession(id: string): Observable<SessionResponse> {
    return this.http.get<SessionResponse>(`${this.baseUrl}/sessions/${id}`);
  }

  logSet(sessionId: string, request: LogSetRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/sessions/${sessionId}/sets`, request);
  }

  addSessionExercise(sessionId: string, exerciseId: string, plannedSets = 3): Observable<SessionExerciseDto> {
    return this.http.post<SessionExerciseDto>(
      `${this.baseUrl}/sessions/${sessionId}/exercises`,
      { exerciseId, plannedSets }
    );
  }

  removeSessionExercise(sessionId: string, exerciseId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sessions/${sessionId}/exercises/${exerciseId}`);
  }

  getSessions(from?: string, to?: string): Observable<SessionResponse[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<SessionResponse[]>(`${this.baseUrl}/sessions`, { params });
  }

  deleteSession(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sessions/${id}`);
  }

  getVolumeReport(from: string, to: string, groupBy = 'muscle'): Observable<VolumeReportDto> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('groupBy', groupBy);
    return this.http.get<VolumeReportDto>(`${this.baseUrl}/volume/report`, { params });
  }

  getProgressionRecommendation(muscleLabel: MuscleLabel): Observable<ProgressionRecommendation> {
    const params = new HttpParams().set('muscleLabel', muscleLabel);
    return this.http.get<ProgressionRecommendation>(`${this.baseUrl}/progression/recommendation`, { params });
  }

  getNextSessionType(muscleLabel: MuscleLabel): Observable<{ muscleLabel: string; nextSessionType: string }> {
    const params = new HttpParams().set('muscleLabel', muscleLabel);
    return this.http.get<{ muscleLabel: string; nextSessionType: string }>(
      `${this.baseUrl}/progression/next-type`, { params }
    );
  }
}
