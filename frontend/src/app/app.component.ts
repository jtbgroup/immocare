import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { VersionService } from './core/services/version.service';

@Component({
  selector: 'app-root',
  template: `
    <div class="app-container">
      <header class="app-header">
        <div class="app-header__brand">
          <h1>ImmoCare</h1>
          <p>Property Management System</p>
        </div>
        <nav class="app-nav">
          <a routerLink="/buildings" routerLinkActive="app-nav__link--active" class="app-nav__link">
            🏢 Buildings
          </a>
          <a routerLink="/users" routerLinkActive="app-nav__link--active" class="app-nav__link">
            👥 Users
          </a>
          <button class="app-nav__logout" (click)="logout()">Logout</button>
        </nav>
      </header>
      <main class="app-content">
        <router-outlet></router-outlet>
      </main>
      <footer class="app-footer">
        <span>v{{ version }}</span>
      </footer>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .app-header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 1rem 2rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .app-header__brand h1 {
      margin: 0;
      font-size: 1.75rem;
      font-weight: 600;
    }

    .app-header__brand p {
      margin: 0.15rem 0 0 0;
      font-size: 0.85rem;
      opacity: 0.85;
    }

    .app-nav {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .app-nav__link {
      color: rgba(255,255,255,0.85);
      text-decoration: none;
      padding: 0.5rem 1rem;
      border-radius: 6px;
      font-size: 0.9rem;
      font-weight: 500;
      transition: background 0.2s, color 0.2s;
    }

    .app-nav__link:hover {
      background: rgba(255,255,255,0.15);
      color: white;
      text-decoration: none;
    }

    .app-nav__link--active {
      background: rgba(255,255,255,0.2);
      color: white;
    }

    .app-nav__logout {
      margin-left: 0.5rem;
      padding: 0.5rem 1rem;
      background: rgba(255,255,255,0.15);
      color: white;
      border: 1px solid rgba(255,255,255,0.3);
      border-radius: 6px;
      font-size: 0.9rem;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }

    .app-nav__logout:hover {
      background: rgba(255,255,255,0.25);
    }

    .app-content {
      flex: 1;
      background: #f5f7fa;
    }

    .app-footer {
      text-align: center;
      padding: 0.4rem;
      font-size: 0.7rem;
      color: #bbb;
      background: #f5f7fa;
    }
  `]
})
export class AppComponent implements OnInit {
  title = 'ImmoCare';
  version = '…';

  constructor(
    private authService: AuthService,
    private router: Router,
    private versionService: VersionService
  ) {}

  ngOnInit(): void {
    this.versionService.getVersion().subscribe(v => this.version = v);
  }

  logout(): void {
    this.authService.logout();
  }
}
