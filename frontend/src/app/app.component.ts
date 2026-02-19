import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  template: `
    <div class="app-container">
      <header class="app-header">
        <h1>ImmoCare</h1>
        <p>Property Management System</p>
      </header>
      <main class="app-content">
        <router-outlet></router-outlet>
      </main>
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
      padding: 1.5rem 2rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    
    .app-header h1 {
      margin: 0;
      font-size: 2rem;
      font-weight: 600;
    }
    
    .app-header p {
      margin: 0.25rem 0 0 0;
      font-size: 0.9rem;
      opacity: 0.9;
    }
    
    .app-content {
      flex: 1;
      background: #f5f7fa;
    }
  `]
})
export class AppComponent {
  title = 'ImmoCare';
}
