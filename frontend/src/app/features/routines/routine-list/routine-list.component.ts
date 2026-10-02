import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RoutineService } from '../../../core/services/routine.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { RoutineResponse } from '../../../core/models/workout.model';
import { MUSCLE_LABEL_OPTIONS } from '../../../core/models/enums';

@Component({
  selector: 'app-routine-list',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Mis Rutinas</h1>
        <a routerLink="/routines/new" class="btn btn-primary btn-new">
          <i class="fa-solid fa-plus"></i> Nueva
        </a>
      </header>

      <main class="page-body">
        @if (loading) {
          <div class="text-center py-5">
            <div class="spinner-border text-primary"></div>
          </div>
        } @else if (routines.length === 0) {
          <div class="empty-state">
            <i class="fa-solid fa-list-check empty-state__icon"></i>
            <h3>Sin rutinas</h3>
            <p>Crea una rutina y planifica tus entrenamientos</p>
            <a routerLink="/routines/new" class="btn btn-primary">Crear rutina</a>
          </div>
        } @else {
          @for (routine of routines; track routine.id) {
            <div class="routine-card">
              <div class="routine-card__main">
                <div class="routine-card__header">
                  <span class="routine-card__name">{{ routine.name }}</span>
                  @if (routine.sessionType) {
                    <span class="routine-card__badge">
                      {{ routine.sessionType === 'STRENGTH' ? 'Fuerza' : 'Hipertrofia' }}
                    </span>
                  }
                </div>
                <div class="routine-card__meta">
                  @if (routine.muscleLabel) {
                    <span><i class="fa-solid fa-dumbbell"></i> {{ muscleLabelMap[routine.muscleLabel] || routine.muscleLabel }}</span>
                  }
                  <span><i class="fa-solid fa-layer-group"></i> {{ routine.exercises.length }} ejercicios</span>
                </div>
                <div class="routine-card__actions">
                  <a [routerLink]="['/workout/start']" [queryParams]="{ routine: routine.id }" class="btn-action btn-action--train">
                    <i class="fa-solid fa-play"></i> Entrenar
                  </a>
                  <a [routerLink]="['/routines', routine.id, 'edit']" class="btn-action">
                    <i class="fa-solid fa-pen"></i> Editar
                  </a>
                  <button type="button" class="btn-action btn-action--danger" (click)="requestDelete(routine)">
                    <i class="fa-solid fa-trash"></i> Eliminar
                  </button>
                </div>
              </div>
            </div>
          }
        }
      </main>

      <app-navbar></app-navbar>

      @if (routineToDelete) {
        <div class="modal-overlay" (click)="cancelDelete()">
          <div class="modal-dialog" (click)="$event.stopPropagation()">
            <h3>Eliminar rutina?</h3>
            <p>La rutina <strong>{{ routineToDelete.name }}</strong> se eliminara permanentemente.</p>
            <div class="modal-dialog__actions">
              <button type="button" class="btn btn-outline-secondary" (click)="cancelDelete()">Cancelar</button>
              <button type="button" class="btn btn-danger" (click)="confirmDelete()" [disabled]="deleting">
                @if (deleting) {
                  <span class="spinner-border spinner-border-sm me-1"></span>
                }
                Eliminar
              </button>
            </div>
          </div>
        </div>
      }
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
    .btn-new { border-radius: 10px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
    .page-body { flex: 1; overflow-y: auto; padding: 12px 16px 80px; }

    .empty-state {
      text-align: center; padding: 60px 20px; color: #6B7280;
      &__icon { font-size: 3rem; color: #D1D5DB; margin-bottom: 16px; }
      h3 { font-weight: 700; color: #111827; margin: 12px 0 4px; }
      p { font-size: 0.9rem; margin-bottom: 20px; }
    }

    .routine-card {
      background: #fff; border-radius: 12px; padding: 14px;
      margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.06);
      &__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; gap: 8px; }
      &__name { font-weight: 700; font-size: 0.95rem; color: #111827; }
      &__badge { font-size: 0.65rem; font-weight: 600; background: #4F46E5; color: #fff; padding: 2px 8px; border-radius: 20px; white-space: nowrap; }
      &__meta { display: flex; gap: 14px; font-size: 0.75rem; color: #6B7280; margin-bottom: 10px;
        i { margin-right: 3px; }
      }
      &__actions { display: flex; gap: 8px; flex-wrap: wrap; }
    }

    .btn-action {
      display: inline-flex; align-items: center; gap: 5px;
      font-size: 0.75rem; font-weight: 600; padding: 6px 10px;
      border-radius: 8px; border: 1px solid #E5E7EB; background: #fff;
      color: #374151; cursor: pointer; text-decoration: none;
      &:hover { border-color: #4F46E5; color: #4F46E5; }
      &--train { background: #4F46E5; border-color: #4F46E5; color: #fff;
        &:hover { background: #4338CA; color: #fff; }
      }
      &--danger { color: #DC2626; border-color: #FCA5A5;
        &:hover { background: #FEF2F2; color: #DC2626; border-color: #DC2626; }
      }
    }

    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0, 0, 0, 0.5);
      display: flex; align-items: center; justify-content: center;
      z-index: 200; padding: 20px; backdrop-filter: blur(4px);
    }
    .modal-dialog {
      // Bootstrap deja .modal-dialog con pointer-events: none (espera .modal-content)
      pointer-events: auto;
      background: #fff; border-radius: 16px; padding: 28px 24px;
      width: 100%; max-width: 360px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
      h3 { font-size: 1.1rem; font-weight: 700; color: #111827; margin: 0 0 8px; }
      p { font-size: 0.9rem; color: #6B7280; margin: 0 0 20px; line-height: 1.5;
        strong { color: #374151; }
      }
      &__actions { display: flex; gap: 10px;
        .btn { flex: 1; padding: 10px; border-radius: 10px; font-weight: 600; font-size: 0.9rem; }
      }
    }
  `]
})
export class RoutineListComponent implements OnInit {
  routines: RoutineResponse[] = [];
  loading = true;
  routineToDelete: RoutineResponse | null = null;
  deleting = false;

  muscleLabelMap: Record<string, string> = Object.fromEntries(
    MUSCLE_LABEL_OPTIONS.map(opt => [opt.value, opt.label])
  );

  constructor(private routineService: RoutineService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.routineService.list().subscribe({
      next: (data) => { this.routines = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  requestDelete(routine: RoutineResponse): void {
    this.routineToDelete = routine;
  }

  cancelDelete(): void {
    this.routineToDelete = null;
  }

  confirmDelete(): void {
    if (!this.routineToDelete) return;
    this.deleting = true;
    this.routineService.delete(this.routineToDelete.id).subscribe({
      next: () => {
        this.routines = this.routines.filter(r => r.id !== this.routineToDelete!.id);
        this.routineToDelete = null;
        this.deleting = false;
      },
      error: () => this.deleting = false
    });
  }
}
