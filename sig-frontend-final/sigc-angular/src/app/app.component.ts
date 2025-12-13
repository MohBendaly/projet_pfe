import { Component, effect, Inject, Injectable, Optional, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/layout/header/header.component'; // Importer le header
import { FooterComponent } from './shared/layout/footer/footer.component'; // Importer le footer
import { ProgressSpinnerModule } from 'primeng/progressspinner'; // Pour indicateur de chargement (optionnel)
import { ToastModule } from 'primeng/toast'; // Pour les messages/notifications
import { MessageService } from 'primeng/api'; // Provider pour Toast
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { toSignal } from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import { AuthService } from './core/services/auth.service';
import { Router } from 'express';
@Component({
  selector: 'app-root',
  standalone: true,
  // Importer RouterOutlet et vos composants de layout
  imports: [
      RouterOutlet,
      HeaderComponent,
      FooterComponent,
      ProgressSpinnerModule,
      MatSlideToggleModule, // Optionnel
      ToastModule // Pour les notifications
  ],
  providers: [MessageService], // Ajouter MessageService ici pour qu'il soit dispo partout
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']

})

export class AppComponent {
  title = 'sigc-frontend';

  constructor(
    @Optional() @Inject(PLATFORM_ID)  private platformId: Object,
    private authService: AuthService,
    private messageService: MessageService
  ) {
     if (isPlatformBrowser(this.platformId)) {
      this.authService.isLoggedIn();
    
    // Initial check
    this.authService.isLoggedIn();
    this.authService.isAuthenticated$.subscribe(isAuthenticated => {
      console.log('Auth state changed:', isAuthenticated);
      // Add your side effects here
      if (!isAuthenticated) {
        // this.router.navigate(['/login']); // Example redirect
      }
    });
  }
}
}
  // Ajouter une logique pour un indicateur de chargement global si nécessaire
  // constructor(private messageService: MessageService) {} // Injecter si besoin pour tests
