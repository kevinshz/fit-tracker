import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component')
      .then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component')
      .then(m => m.RegisterComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'workout', pathMatch: 'full' },
      {
        path: 'workout',
        loadComponent: () => import('./features/workout/workout-history/workout-history.component')
          .then(m => m.WorkoutHistoryComponent)
      },
      {
        path: 'workout/start',
        loadComponent: () => import('./features/workout/start-session/start-session.component')
          .then(m => m.StartSessionComponent)
      },
      {
        path: 'workout/session/:id',
        loadComponent: () => import('./features/workout/workout-session/workout-session.component')
          .then(m => m.WorkoutSessionComponent)
      },
      {
        path: 'exercises',
        loadComponent: () => import('./features/exercises/exercise-list/exercise-list.component')
          .then(m => m.ExerciseListComponent)
      },
      {
        path: 'routines',
        loadComponent: () => import('./features/routines/routine-list/routine-list.component')
          .then(m => m.RoutineListComponent)
      },
      {
        path: 'routines/new',
        loadComponent: () => import('./features/routines/routine-form/routine-form.component')
          .then(m => m.RoutineFormComponent)
      },
      {
        path: 'routines/:id/edit',
        loadComponent: () => import('./features/routines/routine-form/routine-form.component')
          .then(m => m.RoutineFormComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component')
          .then(m => m.ProfileComponent)
      },
    ]
  },
  { path: '**', redirectTo: '' }
];
