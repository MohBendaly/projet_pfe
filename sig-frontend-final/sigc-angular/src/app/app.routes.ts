import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

// Importer les composants (ou utiliser le lazy loading pour de meilleures performances)
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { OfferListComponent } from './features/jobs/offer-list/offer-list.component';
import { OfferDetailComponent } from './features/jobs/offer-detail/offer-detail.component';
import { MyApplicationsComponent } from './features/applications/my-applications/my-applications.component';
import { OfferApplicationsComponent } from './features/applications/offer-applications/offer-applications.component';
import { InterviewChatComponent } from './features/interviews/interview-chat/interview-chat.component';
import { InterviewDetailComponent } from './features/interviews/interview-detail/interview-detail.component';
import { ProfileViewComponent } from './features/profile/profile-view/profile-view.component';
import { ProfileEditComponent } from './features/profile/profile-edit/profile-edit.component';
import { SkillManagementComponent } from './features/admin/skill-management/skill-management.component';
import { OfferFormComponent } from './features/jobs/offer-form/offer-form.component';
// import { NotFoundComponent } from './shared/components/not-found/not-found.component'; // Si créé

export const routes: Routes = [
  // Publiques
  { path: 'login', component: LoginComponent, title: 'Connexion - SIGC' },
  { path: 'register', component: RegisterComponent, title: 'Inscription - SIGC' },
  { path: 'offers', component: OfferListComponent, title: 'Offres d\'emploi - SIGC' },
  { path: 'offers/:id', component: OfferDetailComponent, title: 'Détail Offre - SIGC' },

  // Protégées Candidat
  {
    path: 'candidate', canActivate: [authGuard, roleGuard], data: { expectedRoles: ['ROLE_CANDIDATE'] }, children: [
      { path: 'profile', component: ProfileViewComponent, title: 'Mon Profil - SIGC' },
      { path: 'profile/edit', component: ProfileEditComponent, title: 'Modifier Profil - SIGC' },
      { path: 'applications', component: MyApplicationsComponent, title: 'Mes Candidatures - SIGC' },
      { path: 'interviews/:id/chat', component: InterviewChatComponent, title: 'Entretien - SIGC' },
      { path: 'interviews/:id/details', component: InterviewDetailComponent, title: 'Détail Entretien - SIGC' },
      { path: '', redirectTo: 'profile', pathMatch: 'full' } // Redirection par défaut candidat
    ]
  },
  // Protégées Recruteur/Admin
  {
    path: 'recruiter', canActivate: [authGuard, roleGuard], data: { expectedRoles: ['ROLE_RECRUITER', 'ROLE_ADMIN'] }, children: [
      { path: 'offers/new', component: OfferFormComponent, title: 'Créer Offre - SIGC' },
      { path: 'offers/:id/edit', component: OfferFormComponent, title: 'Modifier Offre - SIGC' },
      { path: 'offers/:id/applications', component: OfferApplicationsComponent, title: 'Candidatures Offre - SIGC' },
      { path: 'interviews/:id/details', component: InterviewDetailComponent, title: 'Détail Entretien (Recruteur) - SIGC' },
       { path: '', redirectTo: 'offers/new', pathMatch: 'full' } // Redirection par défaut recruteur
    ]
  },
   // Protégées Admin
   {
    path: 'admin', canActivate: [authGuard, roleGuard], data: { expectedRoles: ['ROLE_ADMIN'] }, children: [
      { path: 'skills', component: SkillManagementComponent, title: 'Gestion Compétences - SIGC' },
       { path: '', redirectTo: 'skills', pathMatch: 'full' } // Redirection par défaut admin
    ]
  },

  // Redirection & Not Found
  { path: '', redirectTo: '/offers', pathMatch: 'full' },
  // { path: '**', component: NotFoundComponent, title: 'Page Non Trouvée' } // Utiliser un composant 404
   { path: '**', redirectTo: '/offers' } // Ou simple redirection
];