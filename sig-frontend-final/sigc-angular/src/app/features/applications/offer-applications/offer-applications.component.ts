// offer-applications.component.ts
import { Component, inject, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable, BehaviorSubject, switchMap, catchError, of, tap } from 'rxjs';
import { JobApplicationService } from '../job-application.service';
import { JobApplication } from '../job-application.model';
import { ApplicationStatus, getApplicationStatusLabel, getApplicationStatusSeverity } from '../application-status.enum';
import { InterviewService } from '../../interviews/interview.service'; // Pour planifier entretien
import { PagedResult } from '../../../core/models/paged-result.model'; // Ajustez le chemin si nécessaire
// PrimeNG
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown'; // Pour changer statut
import { FormsModule } from '@angular/forms'; // Pour ngModel avec Dropdown
import { TooltipModule } from 'primeng/tooltip';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { CardModule } from 'primeng/card';
import { MessageService } from 'primeng/api';
import { ProgressSpinner } from 'primeng/progressspinner';

@Component({
  selector: 'app-offer-applications',
  standalone: true,
  imports: [ CommonModule, RouterModule, TableModule, TagModule, ButtonModule, DropdownModule, FormsModule, TooltipModule, PaginatorModule, CardModule,ProgressSpinner ],
  templateUrl: './offer-applications.component.html',
  styleUrls: ['./offer-applications.component.scss']
})
export class OfferApplicationsComponent implements OnInit {
  @Input() id!: string; // Offer ID from route

  private applicationService = inject(JobApplicationService);
  private interviewService = inject(InterviewService); // Injecter pour planifier
  private messageService = inject(MessageService);

  applicationsResult$: Observable<PagedResult<JobApplication> | null> | undefined;
  isLoading = false;
  offerId: number | undefined;

  // Pour le dropdown de statut
  availableStatuses = Object.values(ApplicationStatus).map(value => ({ label: getApplicationStatusLabel(value), value: value }));

  // Pagination
  first: number = 0;
  rows: number = 10;
  totalRecords: number = 0;
  private refreshApplications = new BehaviorSubject<void>(undefined);

  // Exposer les helpers
  getApplicationStatusLabel = getApplicationStatusLabel;
  getApplicationStatusSeverity = getApplicationStatusSeverity;
   ApplicationStatus = ApplicationStatus;

  ngOnInit(): void {
    if (this.id) {
      this.offerId = parseInt(this.id, 10);
      if (!isNaN(this.offerId)) {
        this.loadApplications();
      } else {
         console.error("Invalid offer ID");
         // Redirection ?
      }
    }
  }

  loadApplications(): void {
    if (!this.offerId) return;
    this.isLoading = true;
    const currentPage = this.first / this.rows;

    this.applicationsResult$ = this.refreshApplications.pipe(
      switchMap(() => this.applicationService.getApplicationsByOfferId(this.offerId!, currentPage, this.rows).pipe(
        tap(result => {
             this.isLoading = false;
             this.totalRecords = result.totalElements;
        }),
        catchError(error => {
          this.isLoading = false;
          console.error('Error loading applications for offer:', error);
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les candidatures.' });
          return of(null);
        })
      ))
    );
  }

   onPageChange(event: any): void {
   
          this.first = event.first;
          this.rows = event.rows;
          this.refreshApplications.next(); // Déclencher le rechargement avec la nouvelle page/taille
      
   }

  // Changer le statut d'une application
  onStatusChange(applicationId: number, newStatus: ApplicationStatus): void {
     console.log(`Changing status for ${applicationId} to ${newStatus}`);
     this.applicationService.updateApplicationStatus(applicationId, newStatus).subscribe({
         next: (updatedApp) => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.' });
              // Recharger la liste pour refléter le changement (ou mettre à jour localement)
              this.refreshApplications.next();
         },
         error: (err) => {
             console.error("Error updating status", err);
             this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de mettre à jour le statut.' });
             // Recharger pour annuler le changement visuel dans le dropdown
              this.refreshApplications.next();
         }
     });
  }

  // Planifier un entretien
  scheduleInterview(applicationId: number): void {
      console.log(`Scheduling interview for application ${applicationId}`);
      // Idéalement, ouvrir un modal pour configurer date/heure, mais pour l'instant, on appelle directement
      this.interviewService.scheduleInterview(applicationId, {}).subscribe({ // Body vide pour planification simple
            next: (interview) => {
                 this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Entretien planifié (ID: ${interview.id}).` });
                  this.refreshApplications.next(); // Recharger pour voir le statut mis à jour
            },
            error: (err) => {
                 console.error("Error scheduling interview", err);
                  let detail = 'Impossible de planifier l\'entretien.';
                  if (err.error?.message) { detail = err.error.message; }
                 this.messageService.add({ severity: 'error', summary: 'Erreur', detail: detail });
            }
      });
  }
}