import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
  ) => {

  const authService = inject(AuthService);
  const router = inject(Router);
  const expectedRoles: string[] = route.data['expectedRoles'] || []; // Récupérer les rôles attendus depuis les données de la route

  const userRoles = authService.getUserRoles();
  const hasRequiredRole = expectedRoles.some(role => userRoles.includes(role));

  if (authService.isLoggedIn() && hasRequiredRole) {
    console.log(`RoleGuard: Access granted for roles ${expectedRoles}`);
    return true; // L'utilisateur est connecté ET a au moins un des rôles requis
  } else {
    console.log(`RoleGuard: Access denied. User roles: ${userRoles}, Expected roles: ${expectedRoles}`);
    // Rediriger vers une page non autorisée ou la page d'accueil/login
    router.navigate(['/offers']); // Ou '/unauthorized' ou '/login'
    return false;
  }
};