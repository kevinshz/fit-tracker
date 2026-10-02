import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar-bottom">
      <a routerLink="/workout" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }"
         class="navbar-bottom__item">
        <i class="fa-solid fa-dumbbell"></i>
        <span>Entrenar</span>
      </a>
      <a routerLink="/exercises" routerLinkActive="active"
         class="navbar-bottom__item">
        <i class="fa-solid fa-book-open"></i>
        <span>Ejercicios</span>
      </a>
      <a routerLink="/routines" routerLinkActive="active"
         class="navbar-bottom__item">
        <i class="fa-solid fa-list-check"></i>
        <span>Rutinas</span>
      </a>
      <a routerLink="/profile" routerLinkActive="active"
         class="navbar-bottom__item">
        <i class="fa-solid fa-user"></i>
        <span>Perfil</span>
      </a>
    </nav>
  `,
  styles: [`
    .navbar-bottom {
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      display: flex;
      justify-content: space-around;
      align-items: center;
      height: 60px;
      background: #fff;
      border-top: 1px solid #E5E7EB;
      z-index: 1000;
      padding-bottom: env(safe-area-inset-bottom, 0);
    }

    .navbar-bottom__item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
      text-decoration: none;
      color: #9CA3AF;
      font-size: 0.65rem;
      font-weight: 500;
      transition: color 0.2s;
      padding: 6px 12px;

      i { font-size: 1.2rem; }

      &.active {
        color: #4F46E5;
        i { font-weight: 700; }
      }
    }
  `]
})
export class NavbarComponent {}
