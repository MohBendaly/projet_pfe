import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router'; // Importer RouterModule pour routerLink
import { Subscription } from 'rxjs';
import { MenuItem } from 'primeng/api'; // Interface PrimeNG pour les menus
import { MenubarModule } from 'primeng/menubar'; // Module PrimeNG MenuBar
import { ButtonModule } from 'primeng/button'; // Module PrimeNG Button
import { AvatarModule } from 'primeng/avatar'; // Module PrimeNG Avatar (optionnel)
import { AuthService } from '../../../core/services/auth.service';
import { TooltipModule } from 'primeng/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
       MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    CommonModule,
    RouterModule, // Importer pour utiliser routerLink
    MenubarModule,
    ButtonModule,
    AvatarModule,
    TooltipModule
    
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  template: `
    <mat-icon>error_outline</mat-icon>`
})
export class HeaderComponent implements OnInit, OnDestroy {
  authService = inject(AuthService); // Injection moderne
  router = inject(Router);

  menuItems: MenuItem[] = [];
  isLoggedIn = false;
  username: string | null = null;
  userRoles: string[] = [];
  error: string | null = null;

  private authSubscription: Subscription | undefined;
 
  ngOnInit(): void {
    this.authService.isAuthenticated$.subscribe(isAuth => {
    console.log('Auth status changed:', isAuth);
    // Update your UI accordingly
  });
    this.authSubscription = this.authService.isAuthenticated$.subscribe(loggedIn => {
      this.isLoggedIn = loggedIn;
      this.userRoles = this.authService.getUserRoles(); // Mettre à jour les rôles // Mettre à jour le nom d'utilisateur
      this.updateMenuItems(); // Mettre à jour les éléments du menu en fonction de l'état et des rôles
    });
  }

  ngOnDestroy(): void {
    this.authSubscription?.unsubscribe();
  }

  updateMenuItems(): void {
    const baseItems: MenuItem[] = [
      { label: 'Offres d\'emploi', icon: 'pi pi-fw pi-briefcase', routerLink: ['/offers'] }
    ];

    const candidateItems: MenuItem[] = [
      { label: 'Mes Candidatures', icon: 'pi pi-fw pi-file', routerLink: ['/candidate/applications'] },
      { label: 'Mon Profil', icon: 'pi pi-fw pi-user', routerLink: ['/candidate/profile'] }
    ];

    const recruiterItems: MenuItem[] = [
       { label: 'Publier une offre', icon: 'pi pi-fw pi-plus', routerLink: ['/recruiter/offers/new'] },
       // Ajouter un lien pour voir toutes les offres à gérer ?
       // { label: 'Gérer les offres', icon: 'pi pi-fw pi-list', routerLink: ['/recruiter/manage-offers'] }, // Exemple
    ];

     const adminItems: MenuItem[] = [
       { label: 'Gestion Compétences', icon: 'pi pi-fw pi-cog', routerLink: ['/admin/skills'] },
       // Ajouter d'autres liens admin
     ];


    let finalItems = [...baseItems];

    if (this.isLoggedIn) {
         if (this.hasRole('ROLE_CANDIDATE')) {
             finalItems = [...finalItems, ...candidateItems];
         }
         if (this.hasRole('ROLE_RECRUITER')) {
              finalItems = [...finalItems, ...recruiterItems];
         }
          if (this.hasRole('ROLE_ADMIN')) {
               finalItems = [...finalItems, ...adminItems];
               // L'admin a souvent aussi les droits recruteur, on peut éviter de dupliquer si adminItems inclut recruiterItems logiquement
         }
    }

    this.menuItems = finalItems;
  }

  // Helper pour vérifier les rôles
  hasRole(role: string): boolean {
      return this.userRoles.includes(role);
  }

  logout(): void {
    this.authService.logout();
  }
 clearError() {
    this.error = null;
  }
}