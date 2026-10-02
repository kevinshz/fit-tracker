import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExerciseService } from '../../../core/services/exercise.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { ExerciseSummary, PageResponse } from '../../../core/models/exercise.model';
import { muscleGroupLabel, equipmentLabel } from '../../../core/models/enums';

@Component({
  selector: 'app-exercise-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Ejercicios</h1>
      </header>

      <div class="search-bar">
        <i class="fa-solid fa-magnifying-glass"></i>
        <input type="text" placeholder="Buscar ejercicio..." [(ngModel)]="searchQuery"
               (input)="onSearch()">
      </div>

      <main class="page-body">
        @if (loading) {
          <div class="text-center py-5"><div class="spinner-border text-primary"></div></div>
        } @else if (exercises.length === 0) {
          <div class="empty-state">
            <i class="fa-solid fa-book-open"></i>
            <p>No se encontraron ejercicios</p>
          </div>
        } @else {
          @for (ex of exercises; track ex.id) {
            <div class="exercise-card">
              <div class="exercise-card__info">
                <h4>{{ ex.name }}</h4>
                <span class="exercise-card__meta">{{ labelMuscle(ex.primaryMuscle) }} &middot; {{ labelEquipment(ex.equipment) }}</span>
              </div>
              @if (ex.isCustom) {
                <span class="exercise-card__custom">Personalizado</span>
              }
            </div>
          }
        }
      </main>

      <app-navbar></app-navbar>
    </div>
  `,
  styles: [`
    .page-container { height: 100%; display: flex; flex-direction: column; }
    .page-header {
      padding: 16px; padding-top: calc(16px + env(safe-area-inset-top, 0));
      background: #fff; border-bottom: 1px solid #E5E7EB;
      h1 { font-size: 1.3rem; font-weight: 700; margin: 0; }
    }
    .search-bar {
      display: flex; align-items: center; gap: 10px; padding: 10px 16px;
      background: #fff; border-bottom: 1px solid #E5E7EB;
      i { color: #9CA3AF; }
      input { border: none; outline: none; flex: 1; font-size: 0.95rem; background: transparent; }
    }
    .page-body { flex: 1; overflow-y: auto; padding: 8px 16px 80px; }
    .empty-state { text-align: center; padding: 60px 20px; color: #9CA3AF;
      i { font-size: 2.5rem; margin-bottom: 12px; }
    }
    .exercise-card {
      display: flex; align-items: center; justify-content: space-between;
      padding: 14px; background: #fff; border-radius: 10px; margin-bottom: 8px;
      box-shadow: 0 1px 2px rgba(0,0,0,0.04);
      &__info h4 { font-size: 0.9rem; font-weight: 600; margin: 0 0 2px; }
      &__meta { font-size: 0.75rem; color: #6B7280; }
      &__custom { font-size: 0.6rem; background: #FEF3C7; color: #92400E; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
    }
  `]
})
export class ExerciseListComponent implements OnInit {
  exercises: ExerciseSummary[] = [];
  loading = true;
  searchQuery = '';
  readonly labelMuscle = muscleGroupLabel;
  readonly labelEquipment = equipmentLabel;
  private searchTimeout: any;

  constructor(private exerciseService: ExerciseService) {}

  ngOnInit(): void {
    this.loadExercises();
  }

  loadExercises(): void {
    this.loading = true;
    this.exerciseService.list(undefined, undefined, 0, 50).subscribe({
      next: (data) => { this.exercises = data.content; this.loading = false; },
      error: () => this.loading = false
    });
  }

  onSearch(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      if (this.searchQuery.trim()) {
        this.loading = true;
        this.exerciseService.search(this.searchQuery).subscribe({
          next: (data) => { this.exercises = data.content; this.loading = false; },
          error: () => this.loading = false
        });
      } else {
        this.loadExercises();
      }
    }, 300);
  }
}
