import { Component, inject, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable, Subscription, catchError, of, finalize, tap } from 'rxjs';
import { CandidateService } from '../candidate.service'; // Assurez-vous que le chemin est correct
import { Candidate } from '../candidate.model'; // Assurez-vous que le chemin est correct
import { AttachmentService } from '../../attachments/attachment.service'; // Assurez-vous que le chemin est correct
import { Attachment } from '../../attachments/attachment.model'; // Assurez-vous que le chemin est correct
import { AuthService } from '../../../core/services/auth.service'; // Assurez-vous que le chemin est correct

// PrimeNG Modules & Components
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag'; // Si vous affichez des tags
import { ChipModule } from 'primeng/chip'; // Pour les compétences
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { FileUpload, FileUploadModule } from 'primeng/fileupload'; // Importer FileUpload pour ViewChild
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast'; // Import nécessaire pour p-toast
import { SkeletonModule } from 'primeng/skeleton';
import { ConfirmDialogModule } from 'primeng/confirmdialog'; // Si bouton supprimer
import { ConfirmationService } from 'primeng/api';     // Si bouton supprimer

@Component({
  selector: 'app-profile-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CardModule,
    ButtonModule,
    TagModule,
    ChipModule,
    ProgressSpinnerModule,
    FileUploadModule, // Module pour p-fileupload
    TooltipModule,
    ToastModule,      // Module pour p-toast
    SkeletonModule,
    ConfirmDialogModule // Pour la confirmation de suppression
  ],
  providers: [ConfirmationService], // Fournir pour p-confirmDialog
  templateUrl: './profile-view.component.html',
  styleUrls: ['./profile-view.component.scss']
})
export class ProfileViewComponent implements OnInit, OnDestroy {
  // --- Injections ---
  private candidateService = inject(CandidateService);
  private attachmentService = inject(AttachmentService);
  public authService = inject(AuthService); // Public si utilisé dans le template
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService); // Pour la suppression

  // Référence au composant FileUpload pour le reset
  @ViewChild('resumeUploader') resumeUploader: FileUpload | undefined;

  // --- State ---
  isLoadingProfile = true;
  isUploading = false;
  isDeleting = false; // État pour la suppression
  currentProfile: Candidate | null = null;
  // Stocker l'ID de l'attachement CV si on le trouve
  resumeAttachmentId: number | null = null;
  resumeDownloadUrl: string | null = null; // Stocker l'URL complète

  // Gérer les abonnements pour le nettoyage
  private profileSubscription: Subscription | undefined;
  private uploadSubscription: Subscription | undefined;
  private deleteSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.loadProfile();
  }

  ngOnDestroy(): void {
    // Se désabonner pour éviter les fuites mémoire
    this.profileSubscription?.unsubscribe();
    this.uploadSubscription?.unsubscribe();
    this.deleteSubscription?.unsubscribe();
  }

  loadProfile(): void {
    this.isLoadingProfile = true;
    this.currentProfile = null; // Réinitialiser avant chargement
    this.resumeAttachmentId = null;
    this.resumeDownloadUrl = null;

    this.profileSubscription = this.candidateService.getMyProfile().pipe(
      finalize(() => this.isLoadingProfile = false)
    ).subscribe({
      next: (profile) => {
        this.currentProfile = profile;
        console.log("Profile loaded:", this.currentProfile);
        // Essayer de trouver l'attachement CV basé sur le nom ou un type spécifique si possible
        // Pour l'instant, on suppose que si resumePath existe, il faut construire l'URL
        if (this.currentProfile?.resumePath) {
            // Utiliser le service pour construire l'URL (plus robuste)
             this.resumeDownloadUrl = this.attachmentService.buildDownloadUrl(this.currentProfile.resumePath);
             console.log("Resume Download URL built:", this.resumeDownloadUrl);
             // TODO: Idéalement, l'API /me du backend devrait renvoyer l'ID et l'URL de l'attachement CV directement
             // Pour l'instant, on ne peut pas facilement récupérer l'ID de l'attachement ici sans faire un autre appel
        }
      },
      error: (err) => {
        console.error("Error loading profile", err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger le profil.' });
        this.currentProfile = null;
      }
    });
  }

  // Gère l'upload du CV via le handler custom de p-fileupload
 /*  onUploadResume(event: { files: File[] }): void {
    // Vérifier si un profil est chargé (contient l'ID)
    if (!this.currentProfile?.id) {
      this.handleUploadOrDeleteError('ID du profil candidat manquant pour l\'upload.');
      return;
    }
    const candidateId = this.currentProfile.id; // Utiliser l'ID chargé

    const file: File | null = event.files?.[0];

    if (file) {
      console.log(`Uploading resume: ${file.name} for candidate ID: ${candidateId}`);
      this.isUploading = true;

      // Supprimer l'ancien CV avant d'uploader le nouveau si un ID est connu
      // Note : nécessite de récupérer l'ID de l'ancien CV lors du loadProfile
      // if (this.resumeAttachmentId) {
      //    // ... logique de suppression de l'ancien ...
      // }

      // Se désabonner de l'upload précédent s'il existe
      this.uploadSubscription?.unsubscribe();

      this.uploadSubscription = this.attachmentService.uploadForCandidate(candidateId, file)
        .pipe(finalize(() => {
            this.isUploading = false;
            this.resumeUploader?.clear(); // Réinitialiser l'uploader PrimeNG
         }))
        .subscribe({
          next: (attachmentDto: Attachment) => { // Type explicite ici
            console.log("Upload successful:", attachmentDto);
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'CV téléversé avec succès !' });
            // Recharger le profil pour mettre à jour le lien/nom du fichier
            this.loadProfile();
          },
          error: (err) => {
            console.error("Upload failed:", err);
            let detail = 'Erreur lors du téléversement du CV.';
            if (err.error?.message) { detail = err.error.message; }
            else if (typeof err.error === 'string') { detail = err.error; }
            this.handleUploadOrDeleteError(detail, 'Échec Upload');
          }
        });
    } else {
         console.warn("No file selected for upload.");
         this.resumeUploader?.clear();
    }
  } */
onUploadResume(event: Event): void {
  const input = event.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) {
    console.warn("Aucun fichier sélectionné.");
    return;
  }
  const file = input.files[0];

  if (!this.currentProfile?.id) {
    this.handleUploadOrDeleteError('ID du profil candidat manquant pour l\'upload.');
    return;
  }

  const candidateId = this.currentProfile.id;
  this.isUploading = true;

  this.uploadSubscription?.unsubscribe();

  this.uploadSubscription = this.attachmentService.uploadForCandidate(candidateId, file).pipe(
    finalize(() => {
      this.isUploading = false;
      // Réinitialiser la valeur de l'input file pour permettre un nouvel upload du même fichier
      input.value = '';
    })
  ).subscribe({
    next: (attachmentDto) => {
      this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'CV téléversé avec succès !' });
      this.loadProfile();
    },
    error: (err) => {
      console.error("Upload failed:", err);
      let detail = 'Erreur lors du téléversement du CV.';
      if (err.error?.message) { detail = err.error.message; }
      else if (typeof err.error === 'string') { detail = err.error; }
      this.handleUploadOrDeleteError(detail, 'Échec Upload');
    }
  });
}
// Utilisez directement confirmDeleteResume() dans votre template pour afficher la boîte de dialogue PrimeNG
// La méthode openDeleteModal n'est plus nécessaire et peut être supprimée.
openDeleteModal(): void {
  const modalElement = document.getElementById('confirmDeleteModal');
  if (modalElement) {
    // Bootstrap 5 modal show
    const modal = new (window as any).bootstrap.Modal(modalElement);
    modal.show();
  }
}
// Confirmer la suppression du CV
confirmDeleteResume(): void {
    // TODO: Il faut récupérer l'ID de l'attachement du CV actuel.
    // Cela devrait idéalement venir de l'API /me ou d'un appel getAttachmentsForCandidate
    // Pour l'exemple, on suppose qu'on l'a dans this.resumeAttachmentId
     const resumeToDeleteId = this.findResumeAttachmentId(); // Méthode à implémenter

     if (!resumeToDeleteId) {
         this.messageService.add({ severity: 'warn', summary: 'Action impossible', detail: 'Impossible de trouver le CV actuel à supprimer.' });
         return;
     }

     this.confirmationService.confirm({
          message: 'Êtes-vous sûr de vouloir supprimer votre CV actuel ?',
          header: 'Confirmation de Suppression',
          icon: 'pi pi-exclamation-triangle',
          acceptLabel: 'Oui, supprimer',
          rejectLabel: 'Non',
          acceptButtonStyleClass: 'p-button-danger',
          rejectButtonStyleClass: 'p-button-text',
          accept: () => {
              this.deleteResume(resumeToDeleteId);
          }
      });
}

  // Supprimer le CV
  deleteResume(attachmentId: number): void {
      this.isDeleting = true;
      this.deleteSubscription?.unsubscribe(); // Annuler suppression précédente si en cours

      this.deleteSubscription = this.attachmentService.deleteAttachment(attachmentId)
        .pipe(finalize(() => this.isDeleting = false))
        .subscribe({
            next: () => {
                 this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'CV supprimé avec succès.' });
                 this.resumeAttachmentId = null; // Reset l'ID local
                 this.resumeDownloadUrl = null; // Reset l'URL locale
                 // Recharger le profil pour refléter la suppression
                 this.loadProfile();
            },
            error: (err) => {
                 console.error("Error deleting resume", err);
                 let detail = 'Erreur lors de la suppression du CV.';
                 if (err.error?.message) { detail = err.error.message; }
                 else if (typeof err.error === 'string') { detail = err.error; }
                  this.messageService.add({ severity: 'error', summary: 'Échec Suppression', detail: detail });
            }
        });
  }

   // Helper pour gérer les erreurs (un peu générique)
   private handleUploadOrDeleteError(detail: string, summary: string = 'Erreur'): void {
        this.messageService.add({ severity: 'error', summary: summary, detail: detail, life: 5000 });
        // Clear l'uploader seulement en cas d'erreur d'upload
        if (summary === 'Échec Upload') {
             this.resumeUploader?.clear();
        }
   }

    // Placeholder - comment trouver l'ID du CV ?
    // Idéalement, l'API /me renvoie les infos du CV (id, path, url)
    // Sinon, il faudrait appeler getAttachmentsForCandidate et filtrer par nom/type ? (moins fiable)
     private findResumeAttachmentId(): number | null {
         console.warn("La logique pour trouver l'ID du CV à supprimer n'est pas implémentée !");
         // Simuler pour test - NE PAS UTILISER EN PRODUCTION SANS VRAIE LOGIQUE
         // if (this.currentProfile?.resumePath?.includes('cv')) return 1; // Très mauvaise idée
         return this.resumeAttachmentId; // Retourne null si non chargé
     }
}