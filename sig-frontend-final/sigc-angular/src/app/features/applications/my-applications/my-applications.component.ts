// my-applications.component.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable, catchError, of, tap } from 'rxjs';
import { JobApplicationService } from '../job-application.service';
import { JobApplication } from '../job-application.model';
import { ApplicationStatus, getApplicationStatusLabel, getApplicationStatusSeverity } from '../application-status.enum'; // Importer les helpers
// En haut de votre fichier .ts (ex: my-applications.component.ts)
type PrimeNGSeverity = "success" | "secondary" | "info" | "warning" | "danger" | "contrast";
// PrimeNG
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip'; // Pour infos au survol
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-my-applications',
  standalone: true,
  imports: [ CommonModule, RouterModule, TableModule, TagModule, ButtonModule, TooltipModule,CardModule, ProgressSpinnerModule ],
  templateUrl: './my-applications.component.html',
  styleUrls: ['./my-applications.component.scss']
})
export class MyApplicationsComponent implements OnInit {
  private applicationService = inject(JobApplicationService);

  applications$: Observable<JobApplication[] | null> | undefined;
  isLoading = false;

  // Exposer les helpers au template
  getApplicationStatusLabel = getApplicationStatusLabel;
  getApplicationStatusSeverity = getApplicationStatusSeverity;
  ApplicationStatus = ApplicationStatus; // Rendre l'enum accessible pour comparaison

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.isLoading = true;
    this.applications$ = this.applicationService.getMyApplications().pipe(
        tap(() => this.isLoading = false),
        catchError(error => {
            this.isLoading = false;
            console.error('Error loading my applications:', error);
            // Afficher message d'erreur
            return of(null); // Retourner null en cas d'erreur
        })
    );
  }

  // Méthode pour retirer une candidature (exemple)
  withdraw(applicationId: number): void {
     // Ajouter une confirmation p-confirmDialog serait mieux
     console.log(`Withdrawing application ${applicationId}`);
     this.applicationService.withdrawApplication(applicationId).subscribe({
          next: () => {
              console.log('Application withdrawn');
              this.loadApplications(); // Recharger la liste
               // Afficher message succès
          },
          error: (err) => {
               console.error('Error withdrawing application', err);
               // Afficher message erreur
          }
     });
  }
}