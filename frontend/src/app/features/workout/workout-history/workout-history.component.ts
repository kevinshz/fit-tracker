import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WorkoutService } from '../../../core/services/workout.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { TimerPipe } from '../../../shared/pipes/timer.pipe';
import { VolumePipe } from '../../../shared/pipes/volume.pipe';
import { SessionResponse } from '../../../core/models/workout.model';

@Component({
  selector: 'app-workout-history',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent, TimerPipe, VolumePipe],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Mis Entrenamientos</h1>
        <a routerLink="/workout/start" class="btn btn-primary btn-start">
          <i class="fa-solid fa-plus"></i> Nuevo
        </a>
      </header>

      <main class="page-body">
        @if (loading) {
          <div class="text-center py-5">
            <div class="spinner-border text-primary"></div>
          </div>
        } @else if (sessions.length === 0) {
          <div class="empty-state">
            <i class="fa-solid fa-dumbbell empty-state__icon"></i>
            <h3>Sin entrenamientos</h3>
            <p>Comienza tu primer entrenamiento</p>
            <a routerLink="/workout/start" class="btn btn-primary">Empezar</a>
          </div>
        } @else {
          @for (session of sessions; track session.id) {
            <a [routerLink]="['/workout/session', session.id]" class="session-card">
              <div class="session-card__header">
                <span class="session-card__type">{{ muscleLabelMap[session.muscleLabel] || session.muscleLabel }}</span>
                <span class="session-card__badge">{{ session.sessionType === 'STRENGTH' ? 'Fuerza' : 'Hiper.' }}</span>
              </div>
              <div class="session-card__meta">
                <span><i class="fa-regular fa-calendar"></i> {{ session.performedAt }}</span>
                <span><i class="fa-solid fa-layer-group"></i> {{ session.sets.length }} series</span>
                <span><i class="fa-solid fa-weight-hanging"></i> {{ calcSessionVolume(session) | volume }}</span>
              </div>
            </a>
          }
        }
      </main>

      <app-navbar></app-navbar>
    </div>
  `,
  styles: [`
    .page-container { height: 100%; display: flex; flex-direction: column; }
    .page-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px; padding-top: calc(16px + env(safe-area-inset-top, 0));
      background: #fff; border-bottom: 1px solid #E5E7EB;
      h1 { font-size: 1.3rem; font-weight: 700; margin: 0; }
    }
    .btn-start { border-radius: 10px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
    .page-body { flex: 1; overflow-y: auto; padding: 12px 16px 80px; }

    .empty-state {
      text-align: center; padding: 60px 20px; color: #6B7280;
      &__icon { font-size: 3rem; color: #D1D5DB; margin-bottom: 16px; }
      h3 { font-weight: 700; color: #111827; margin: 12px 0 4px; }
      p { font-size: 0.9rem; margin-bottom: 20px; }
    }

    .session-card {
      display: block; background: #fff; border-radius: 12px; padding: 14px;
      margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.06);
      text-decoration: none; color: inherit; transition: box-shadow 0.2s;
      &:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
      &__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
      &__type { font-weight: 700; font-size: 0.95rem; color: #111827; }
      &__badge { font-size: 0.65rem; font-weight: 600; background: #4F46E5; color: #fff; padding: 2px 8px; border-radius: 20px; }
      &__meta { display: flex; gap: 14px; font-size: 0.75rem; color: #6B7280;
        i { margin-right: 3px; }
      }
    }
  `]
})
export class WorkoutHistoryComponent implements OnInit {
  sessions: SessionResponse[] = [];
  loading = true;

  muscleLabelMap: Record<string, string> = {
    CHEST_BACK: 'Pecho + Espalda', LEGS: 'Piernas', PUSH: 'Empuje',
    PULL: 'Tirón', SHOULDERS_ARMS: 'Hombros + Brazos', FULL_BODY: 'Full Body'
  };

  constructor(private workoutService: WorkoutService) {}

  ngOnInit(): void {
    this.workoutService.getSessions().subscribe({
      next: (data) => { this.sessions = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  calcSessionVolume(session: SessionResponse): number {
    return session.sets.reduce((sum, s) => sum + (s.volumeKg || 0), 0);
  }
}
