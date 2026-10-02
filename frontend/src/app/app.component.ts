import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="app-shell">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .app-shell {
      height: 100%;
      display: flex;
      flex-direction: column;
    }
  `]
})
export class AppComponent {}
