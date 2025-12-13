import { Component, inject, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable, switchMap, catchError, of, EMPTY, tap, finalize } from 'rxjs';
import { JobOfferService } from '../job-offer.service';
import { JobOffer } from '../job-offer.model';
import { JobApplicationService } from '../../applications/job-application.service'; // Pour postuler
import { AuthService } from '../../../core/services/auth.service';

import { SkeletonModule } from 'primeng/skeleton'; // Importer pour le squelette
import { TooltipModule } from 'primeng/tooltip'; // Importer pour les tooltips
// PrimeNG
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ChipModule } from 'primeng/chip'; // Pour les compétences/requirements
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api'; // Pour les notifications
import { ToastModule } from 'primeng/toast'; // Pour afficher les notifications
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

type PrimeNGSeverity = "success" | "secondary" | "info" | "warn" | "danger" | "contrast";
//import { Nl2brPipe } from '../../../shared/pipes/nl2br.pipe'; // Ajustez le chemin

@Component({
  selector: 'app-offer-detail',
  standalone: true,
  imports: [
      CommonModule,
  RouterModule,
  // PrimeNG
  CardModule, ButtonModule, TagModule, SkeletonModule,
  ChipModule, ProgressSpinnerModule, TooltipModule, ToastModule,
  // Angular Material
  MatCardModule, MatButtonModule, MatIconModule, MatTooltipModule
    
    
    
   ],
   providers: [], // MessageService est fourni globalement via app.config
  templateUrl: './offer-detail.component.html',
  styleUrls: ['./offer-detail.component.scss']
})
export class OfferDetailComponent implements OnInit {
  // Injection via @Input grâce à withComponentInputBinding dans app.config
  @Input() id!: string; // Reçoit l'ID de la route
  private messageService = inject(MessageService); // Injecter

  private jobOfferService = inject(JobOfferService);
  private jobApplicationService = inject(JobApplicationService); // Injecter pour postuler
  private authService = inject(AuthService);
  private router = inject(Router);

  offer$: Observable<JobOffer | null> | undefined;
  isLoading = false;
  applyLoading = false;
  canApply = false; // Le candidat peut-il postuler ?
  isRecruiterOrAdmin = false;

  ngOnInit(): void {
    if (this.id) {
      const offerId = parseInt(this.id, 10);
      if (!isNaN(offerId)) {
          this.isLoading = true;
          this.offer$ = this.jobOfferService.getOfferById(offerId).pipe(
            tap(offer => {
                this.isLoading = false;
                // Vérifier si le candidat peut postuler (connecté, rôle candidat, offre publiée)
                this.canApply = this.authService.isLoggedIn() &&
                                this.authService.hasRole('ROLE_CANDIDATE') &&
                                offer?.status === 'PUBLISHED'; // Ne peut postuler qu'à une offre publiée
                this.isRecruiterOrAdmin = this.authService.hasRole('ROLE_RECRUITER') || this.authService.hasRole('ROLE_ADMIN');
                console.log('Offer loaded:', offer);
            }),
            catchError(error => {
              this.isLoading = false;
              console.error('Error loading offer details:', error);
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les détails de l\'offre.' });
              this.router.navigate(['/offers']); // Rediriger si offre non trouvée
              return of(null); // Retourner null en cas d'erreur
            })
        );
      } else {
         console.error('Invalid offer ID:', this.id);
         this.router.navigate(['/offers']);
      }
    } else {
        console.error('No offer ID provided');
        this.router.navigate(['/offers']);
    }
  }

  getSeverity(status: string | undefined): PrimeNGSeverity | undefined {
    if (!status) return undefined; // Ou 'secondary' comme valeur par défaut

    switch (status) {
      case 'PUBLISHED': return 'success';
      case 'DRAFT': return 'info';
      case 'CLOSED': return 'warn';
      case 'ARCHIVED':
      case 'FILLED': return 'danger';
      default: return 'secondary'; // Valeur par défaut si status inconnu
    }
  }

  applyForJob(offerId: number): void {
      if (!this.canApply) return;

      this.applyLoading = true;
      // On suppose qu'il n'y a pas de lettre de motivation à ce stade (peut être ajoutée via un modal)
      const applicationRequest = { jobOfferId: offerId };

      this.jobApplicationService.applyToOffer(applicationRequest)
        .pipe(finalize(() => this.applyLoading = false))
        .subscribe({
             next: (application) => {
                console.log('Application successful:', application);
                this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Votre candidature a été envoyée !' });
                this.canApply = false; // Empêcher de postuler à nouveau
                // Optionnel: rediriger vers 'Mes candidatures'
                // setTimeout(() => this.router.navigate(['/candidate/applications']), 1500);
             },
             error: (err) => {
                console.error('Error applying for job:', err);
                let detail = 'Erreur lors de l\'envoi de la candidature.';
                 if (err.status === 400 && err.error?.error?.includes('not published')) {
                     detail = 'Vous ne pouvez pas postuler à cette offre car elle n\'est pas publiée.';
                 } else if (err.status === 409) { // Conflit si déjà postulé
                     detail = 'Vous avez déjà postulé à cette offre.';
                     this.canApply = false; // Marquer comme non applicable si déjà postulé
                 } else if (err.error?.message) {
                    detail = err.error.message;
                 } else if (typeof err.error === 'string') {
                    detail = err.error;
                 }
                 this.messageService.add({ severity: 'error', summary: 'Erreur', detail: detail });
             }
        });
  }
}