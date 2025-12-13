import { inject,Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map, take,tap,catchError,switchMap } from 'rxjs/operators';
import { of,Observable } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { TokenStorageService } from '../services/token-storage.service';

// Guard fonctionnel
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const tokenService = inject(TokenStorageService);
  const router = inject(Router);

  

   console.log('AuthGuard: Début de la vérification');

  // Utiliser l'observable pour vérifier l'état d'authentification
  return authService.isAuthenticated$.pipe(
    take(1),
    tap(isAuth => console.log('Auth state:', isAuth)),
    switchMap(isAuth => {
      if (isAuth) {
        console.log('AuthGuard: Accès autorisé - Utilisateur authentifié');
        console.log('Détails du token:', jwtDecode(tokenService.getToken()!));
        return of(true);
      }
      
      // Double vérification au cas où
      const token = tokenService.getToken();
      console.log('Token from storage:', token);
      
      if (token) {
        authService.setAuthState(true);
        return of(true);
      }
      
      console.warn('AuthGuard: Accès refusé - Redirection vers /login');
      console.warn('Raison possible:', !token ? 'Token manquant' : 'Token expiré/invalide');
      router.navigate(['/login'], { queryParams: { returnUrl: state.url }});
      return of(false);
    }),
    catchError(error => {
      console.error('AuthGuard: Erreur lors de la vérification', error);
      router.navigate(['/login']);
      return of(false);
    })
  );
};