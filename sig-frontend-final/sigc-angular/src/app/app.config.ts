// src/app/app.config.ts
import { ApplicationConfig, LOCALE_ID, importProvidersFrom } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
// Supprimer provideAnimations si ng-bootstrap ne le requiert pas explicitement
// import { provideAnimations } from '@angular/platform-browser/animations';

// Importer NgbModule si ng-bootstrap est utilisé
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

// ... config locale ...
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
registerLocaleData(localeFr, 'fr');

import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { AuthService } from './core/services/auth.service';
import { TokenStorageService } from './core/services/token-storage.service';
import { Application } from 'express';

export const appConfig: ApplicationConfig= {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    AuthService,
    TokenStorageService,
    provideHttpClient(withInterceptors([jwtInterceptor])),
    // provideAnimations(), // Supprimer ou garder si ng-bootstrap l'exige
    { provide: LOCALE_ID, useValue: 'fr' },

    // Importer les providers de NgbModule si utilisé
    importProvidersFrom(NgbModule)

    // Supprimer les providers PrimeNG
    // MessageService,
    // ConfirmationService
  ]
};